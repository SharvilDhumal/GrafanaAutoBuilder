package com.example.grafanaautobuilder.client;

import com.example.grafanaautobuilder.config.GrafanaProperties;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class GrafanaClient {
    private final RestTemplate restTemplate;
    private final GrafanaProperties grafanaProperties;

    public GrafanaClient(RestTemplate restTemplate, GrafanaProperties grafanaProperties) {
        this.restTemplate = restTemplate;
        this.grafanaProperties = grafanaProperties;
    }

    public ResponseEntity<String> createOrUpdateDashboard(Map<String, Object> dashboardPayload) {
        String url = grafanaProperties.getUrl();
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("grafana.url is not configured");
        }
        String endpoint = url.replaceAll("/+$", "") + "/api/dashboards/db";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String apiKey = grafanaProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("grafana.apiKey is not configured");
        }
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(dashboardPayload, headers);
        return restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);
    }
}
