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
import java.util.UUID;

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

    public String uploadCsv(Long userId, MultipartFile file) {
        String safeName = sanitize(file.getOriginalFilename());
        String path = "users/" + (userId == null ? "anonymous" : userId) + "/" + UUID.randomUUID() + "-" + safeName;

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
}
