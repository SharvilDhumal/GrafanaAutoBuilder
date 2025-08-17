package com.example.grafanaautobuilder.service.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.serviceKey}")
    private String serviceKey;

    @Value("${supabase.storage.bucket}")
    private String bucket;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder().build();

    public String uploadCsv(Long userId, String username, MultipartFile file) {
        String safeName = sanitize(file.getOriginalFilename());
        // Prefer explicit username/email, then numeric userId, else 'anonymous'
        String owner = (username != null && !username.isBlank())
                ? sanitizePathSegment(username)
                : (userId != null ? String.valueOf(userId) : "anonymous");
        // Keep the exact original filename (sanitized), do not prefix with UUID
        String path = "users/" + owner + "/" + safeName;

        byte[] bytes = toBytes(file);

        String ct = file.getContentType();
        if (ct == null || ct.isBlank()) {
            ct = "text/csv";
        }

        webClient.post()
                .uri(supabaseUrl + "/storage/v1/object/" + bucket + "/" + path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                .header("x-upsert", "true")
                .contentType(MediaType.parseMediaType(ct))
                .bodyValue(bytes)
                .retrieve()
                .toBodilessEntity()
                .block();

        return path;
    }

    // Backward-compatible overload
    public String uploadCsv(Long userId, MultipartFile file) {
        return uploadCsv(userId, null, file);
    }

    public String createSignedUrl(String path, int expiresInSeconds) {
        try {
            String json = webClient.post()
                    .uri(supabaseUrl + "/storage/v1/object/sign/" + bucket + "/" + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("expiresIn", expiresInSeconds))
                    .retrieve()
                    .bodyToMono(String.class)
                    .onErrorResume(err -> Mono.error(new RuntimeException("Supabase sign URL failed", err)))
                    .block();

            JsonNode root = objectMapper.readTree(json);
            JsonNode urlNode = root.get("signedURL");
            if (urlNode == null || urlNode.isNull()) {
                throw new RuntimeException("signedURL missing in response: " + json);
            }
            // Response is a relative path; prepend base URL
            return supabaseUrl + urlNode.asText();
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse Supabase sign response", e);
        }
    }

    private byte[] toBytes(MultipartFile f) {
        try {
            return f.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String sanitize(String name) {
        String n = (name == null || name.isBlank()) ? "file.csv" : name;
        return n.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizePathSegment(String segment) {
        String s = (segment == null || segment.isBlank()) ? "anonymous" : segment;
        // Avoid slashes and unsafe chars in path segment
        return s.replaceAll("[/\\\\]+", "_").replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
