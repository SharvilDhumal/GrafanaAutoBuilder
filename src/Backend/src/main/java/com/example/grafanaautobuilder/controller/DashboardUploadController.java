package com.example.grafanaautobuilder.controller;

import com.example.grafanaautobuilder.config.GrafanaProperties;
import com.example.grafanaautobuilder.service.grafana.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardUploadController {

    private final DashboardService dashboardService;
    private final GrafanaProperties grafanaProperties;
    private static final Logger log = LoggerFactory.getLogger(DashboardUploadController.class);

    public DashboardUploadController(DashboardService dashboardService, GrafanaProperties grafanaProperties) {
        this.dashboardService = dashboardService;
        this.grafanaProperties = grafanaProperties;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // Uncomment if using method-level security
    // @PreAuthorize("isAuthenticated()")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> uploadCsvAndCreateDashboard(@RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "title", required = false) String title) {
        try {
            if (file == null || file.isEmpty()) {
                log.warn("/api/dashboard/upload called without file or with empty file");
                return ResponseEntity.badRequest().body(Map.of("error", "CSV file is required"));
            }
            log.info("Received upload: name='{}', size={} bytes, contentType='{}', title='{}'",
                    file.getOriginalFilename(), file.getSize(), file.getContentType(), title);

            String computedTitle = (title != null && !title.isBlank()) ? title :
                    (stripCsv(file.getOriginalFilename()) + " - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            log.info("Computed dashboard title: {}", computedTitle);

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
            log.info("Dashboard creation request completed, uid={} url={}", uid, dashboardUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error handling /api/dashboard/upload: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Failed to process CSV or create dashboard",
                    "details", e.getMessage()
            ));
        }
    }

    private static String stripCsv(String name) {
        if (name == null) return "Dashboard";
        return name.endsWith(".csv") ? name.substring(0, name.length() - 4) : name;
    }
}
