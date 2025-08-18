// POST /upload → upload a .csv file → saves to Supabase + stores metadata in DB → returns file info.
// GET /signed-url → generates a temporary download link for a stored file. 



package com.example.grafanaautobuilder.controller;

import com.example.grafanaautobuilder.dto.FileMetadataDto;
import com.example.grafanaautobuilder.entity.FileMetadata;
import com.example.grafanaautobuilder.repository.FileMetadataRepository;
import com.example.grafanaautobuilder.entity.User;
import com.example.grafanaautobuilder.service.storage.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final SupabaseStorageService storageService;
    private final FileMetadataRepository fileRepo;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "userId", required = false) Long userId) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "file is required"));
        }
        String originalName = file.getOriginalFilename();
        String name = originalName != null ? originalName.toLowerCase() : "";
        if (!name.endsWith(".csv")) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body(Map.of("error", "only .csv files are allowed"));
        }
        try {
            // Resolve current user (email) if authenticated
            String username = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                Object principal = auth.getPrincipal();
                if (principal instanceof User) {
                    username = ((User) principal).getEmail();
                } else if (auth.getName() != null && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
                    username = auth.getName();
                }
            }

            String objectPath = storageService.uploadCsv(userId, username, file);
            String checksum = DigestUtils.md5DigestAsHex(file.getBytes());

            FileMetadata meta = new FileMetadata(
                    null,
                    userId,
                    "uploads",
                    objectPath,
                    originalName != null ? originalName : "file.csv",
                    file.getSize(),
                    checksum,
                    Instant.now()
            );
            meta = fileRepo.save(meta);

            FileMetadataDto dto = new FileMetadataDto(meta);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "upload failed", "details", e.getMessage()));
        }
    }

    @GetMapping("/signed-url")
    public ResponseEntity<?> signedUrl(@RequestParam("path") String path,
                                       @RequestParam(value = "expires", defaultValue = "3600") int expires) {
        try {
            String url = storageService.createSignedUrl(path, expires);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("Sign URL failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "sign failed", "details", e.getMessage()));
        }
    }
}
