package com.example.grafanaautobuilder.controller;

import com.example.grafanaautobuilder.config.GrafanaProperties;
import com.example.grafanaautobuilder.entity.FileMetadata;
import com.example.grafanaautobuilder.entity.User;
import com.example.grafanaautobuilder.repository.FileMetadataRepository;
import com.example.grafanaautobuilder.service.csv.CsvValidationService;
import com.example.grafanaautobuilder.service.grafana.DashboardService;
import com.example.grafanaautobuilder.service.storage.SupabaseStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardUploadController {

    private final DashboardService dashboardService;
    private final CsvValidationService csvValidationService;
    private final GrafanaProperties grafanaProperties;
    private final SupabaseStorageService storageService;
    private final FileMetadataRepository fileRepo;
    private static final Logger log = LoggerFactory.getLogger(DashboardUploadController.class);

    public DashboardUploadController(DashboardService dashboardService,
                                     GrafanaProperties grafanaProperties,
                                     CsvValidationService csvValidationService,
                                     SupabaseStorageService storageService,
                                     FileMetadataRepository fileRepo) {
        this.dashboardService = dashboardService;
        this.grafanaProperties = grafanaProperties;
        this.csvValidationService = csvValidationService;
        this.storageService = storageService;
        this.fileRepo = fileRepo;
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of("message", "Backend is working"));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // Uncomment if using method-level security
    // @PreAuthorize("isAuthenticated()")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> uploadCsvAndCreateDashboard(@RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "title", required = false) String title) {
        try {
            log.info("Received upload request - file: {}, title: {}", 
                    file != null ? file.getOriginalFilename() : "null", title);
            
            if (file == null || file.isEmpty()) {
                log.warn("/api/dashboard/upload called without file or with empty file");
                return ResponseEntity.badRequest().body(Map.of("error", "CSV file is required"));
            }
            log.info("Received upload: name='{}', size={} bytes, contentType='{}', title='{}'",
                    file.getOriginalFilename(), file.getSize(), file.getContentType(), title);

            String computedTitle = (title != null && !title.isBlank()) ? title :
                    (stripCsv(file.getOriginalFilename()) + " - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            log.info("Computed dashboard title: {}", computedTitle);

            // Resolve current user (email) if authenticated
            String username = null;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User) {
                username = ((User) auth.getPrincipal()).getEmail();
            }

            // Save CSV to Supabase storage and persist metadata (userId unknown -> null)
            String objectPath = storageService.uploadCsv(null, username, file);
            String checksum = DigestUtils.md5DigestAsHex(file.getBytes());
            FileMetadata meta = new FileMetadata(
                    null,
                    null,
                    "uploads",
                    objectPath,
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "file.csv",
                    file.getSize(),
                    checksum,
                    Instant.now()
            );
            meta = fileRepo.save(meta);

            Map<String, Object> result = dashboardService.createDashboardFromCsv(file, computedTitle);
            // Best-effort URL using the uid we sent; Grafana may override, but this still helps UX.
            Map<String, Object> payload = (Map<String, Object>) result.get("requestPayload");
            Map<String, Object> dash = (Map<String, Object>) payload.get("dashboard");
            String uid = dash.get("uid").toString();
            String base = grafanaProperties.getUrl() != null ? grafanaProperties.getUrl().replaceAll("/+$$", "") : "http://localhost:3000";
            String dashboardUrl = base + "/d/" + uid;

            Map<String, Object> response = new HashMap<>();
            response.put("uid", uid);
            response.put("title", dash.get("title"));
            response.put("grafanaUrl", dashboardUrl);
            response.put("grafanaResponse", result.get("grafanaResponse"));
            // also include storage info
            response.put("storageBucket", "uploads");
            response.put("storageObjectPath", objectPath);
            log.info("Dashboard creation request completed, uid={} url={}", uid, dashboardUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error handling /api/dashboard/upload: {}", e.getMessage(), e);
            log.error("Stack trace: ", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process CSV or create dashboard",
                    "details", e.getMessage()
            ));
        }
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> validateCsv(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "CSV file is required"));
            }
            var results = csvValidationService.validate(file);
            return ResponseEntity.ok(Map.of("results", results));
        } catch (Exception e) {
            log.error("Error handling /api/dashboard/validate: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to validate CSV",
                    "details", e.getMessage()
            ));
        }
    }

    private static String stripCsv(String name) {
        if (name == null) return "Dashboard";
        return name.endsWith(".csv") ? name.substring(0, name.length() - 4) : name;
    }
}
