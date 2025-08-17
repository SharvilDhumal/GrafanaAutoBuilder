package com.example.grafanaautobuilder.service.grafana;

import com.example.grafanaautobuilder.dto.PanelConfig;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardBuilder {
    public static final int GRID_COLUMNS = 24;

    public Map<String, Object> buildDashboard(String title, List<Map<String, Object>> panels) {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.put("uid", UUID.randomUUID().toString().replaceAll("-", ""));
        dashboard.put("title", title);
        dashboard.put("schemaVersion", 38);
        dashboard.put("panels", panels);
        List<String> tags = new ArrayList<>();
        dashboard.put("tags", tags);
        // Shared tooltip/crosshair for a modern analytical feel
        dashboard.put("graphTooltip", 1);
        
        // Add time range configuration to fix "no data" issue
        Map<String, Object> time = new HashMap<>();
        time.put("from", "now-7d");
        time.put("to", "now");
        dashboard.put("time", time);
        
        // Add time picker configuration
        Map<String, Object> timepicker = new HashMap<>();
        timepicker.put("refresh_intervals", Arrays.asList("5s", "10s", "30s", "1m", "5m", "15m", "30m", "1h", "2h", "1d"));
        timepicker.put("time_options", Arrays.asList("5m", "15m", "1h", "6h", "12h", "24h", "2d", "7d", "30d"));
        dashboard.put("timepicker", timepicker);
        
        // Add templating for better data selection
        Map<String, Object> templating = new HashMap<>();
        templating.put("list", new ArrayList<>());
        dashboard.put("templating", templating);
        
        // Add refresh configuration
        dashboard.put("refresh", "5m");
        
        // Add timezone
        dashboard.put("timezone", "browser");

        // Business preset: if the title suggests business analytics, apply a more executive-friendly configuration
        if (title != null && title.toLowerCase(Locale.ROOT).contains("business")) {
            // Tag
            tags.add("business");
            // Longer default time range
            time.put("from", "now-1y");
            // Less frequent refresh
            dashboard.put("refresh", "1h");
            // Allow override back into payload
            dashboard.put("time", time);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("dashboard", dashboard);
        payload.put("overwrite", true);
        return payload;
    }

    public List<Map<String, Object>> layoutPanels(List<PanelConfig> configs, PanelJsonBuilder panelBuilder) {
        List<Map<String, Object>> panels = new ArrayList<>();
        int x = 0, y = 0, rowHeight = 0, id = 1;
        for (PanelConfig cfg : configs) {
            int w = (cfg.getW() == null || cfg.getW() <= 0) ? 12 : cfg.getW();
            int h = (cfg.getH() == null || cfg.getH() <= 0) ? 8 : cfg.getH();
            if (x + w > GRID_COLUMNS) {
                // wrap to next row
                x = 0;
                y += rowHeight;
                rowHeight = 0;
            }
            Map<String, Object> panel = panelBuilder.buildPanel(cfg, x, y, id++);
            panels.add(panel);
            x += w;
            rowHeight = Math.max(rowHeight, h);
        }
        return panels;
    }
}
