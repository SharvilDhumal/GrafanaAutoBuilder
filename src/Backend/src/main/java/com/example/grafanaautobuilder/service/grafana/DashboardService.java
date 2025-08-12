package com.example.grafanaautobuilder.service.grafana;

import com.example.grafanaautobuilder.client.GrafanaClient;
import com.example.grafanaautobuilder.dto.PanelConfig;
import com.example.grafanaautobuilder.service.csv.CsvParsingService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final CsvParsingService csvParsingService;
    private final PanelJsonBuilder panelJsonBuilder;
    private final DashboardBuilder dashboardBuilder;
    private final GrafanaClient grafanaClient;

    public DashboardService(CsvParsingService csvParsingService,
                            PanelJsonBuilder panelJsonBuilder,
                            DashboardBuilder dashboardBuilder,
                            GrafanaClient grafanaClient) {
        this.csvParsingService = csvParsingService;
        this.panelJsonBuilder = panelJsonBuilder;
        this.dashboardBuilder = dashboardBuilder;
        this.grafanaClient = grafanaClient;
    }

    public Map<String, Object> createDashboardFromCsv(MultipartFile csvFile, String title) throws IOException {
        List<PanelConfig> configs = csvParsingService.parse(csvFile.getInputStream());
        var panels = dashboardBuilder.layoutPanels(configs, panelJsonBuilder);
        var payload = dashboardBuilder.buildDashboard(title, panels);
        ResponseEntity<String> resp = grafanaClient.createOrUpdateDashboard(payload);

        Map<String, Object> result = new HashMap<>();
        result.put("requestPayload", payload);
        result.put("grafanaResponse", resp.getBody());
        return result;
    }
}
