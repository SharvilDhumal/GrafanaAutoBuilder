package com.example.grafanaautobuilder.controller;

import com.example.grafanaautobuilder.config.GrafanaProperties;
import com.example.grafanaautobuilder.service.grafana.DashboardService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardUploadController {

    private final DashboardService dashboardService;
    private final GrafanaProperties grafanaProperties;

    public DashboardUploadController(DashboardService dashboardService, GrafanaProperties grafanaProperties) {
        this.dashboardService = dashboardService;
        this.grafanaProperties = grafanaProperties;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // Uncomment if using method-level security
    // @PreAuthorize("isAuthenticated()")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> uploadCsvAndCreateDashboard(@RequestPart("file") MultipartFile file,
                                                         @RequestParam(value = "title", required = false) String title) throws IOException {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "CSV file is required"));
        }
        String computedTitle = (title != null && !title.isBlank()) ? title :
                (stripCsv(file.getOriginalFilename()) + " - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        Map<String, Object> result = dashboardService.createDashboardFromCsv(file, computedTitle);
        // Best-effort URL using the uid we sent; Grafana may override, but this still helps UX.
        Map<String, Object> payload = (Map<String, Object>) result.get("requestPayload");
        Map<String, Object> dash = (Map<String, Object>) payload.get("dashboard");
        String uid = dash.get("uid").toString();
        String base = grafanaProperties.getUrl() != null ? grafanaProperties.getUrl().replaceAll("/+$", "") : "http://localhost:3000";
        String dashboardUrl = base + "/d/" + uid;

        Map<String, Object> response = new HashMap<>();
        response.put("uid", uid);
        response.put("title", dash.get("title"));
        response.put("grafanaUrl", dashboardUrl);
        response.put("grafanaResponse", result.get("grafanaResponse"));
        return ResponseEntity.ok(response);
    }

    private static String stripCsv(String name) {
        if (name == null) return "Dashboard";
        return name.endsWith(".csv") ? name.substring(0, name.length() - 4) : name;
    }
}
