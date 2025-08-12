package com.example.grafanaautobuilder.service.grafana;

import com.example.grafanaautobuilder.dto.PanelConfig;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PanelJsonBuilder {

    public Map<String, Object> buildPanel(PanelConfig cfg, int x, int y, int id) {
        int w = (cfg.getW() == null || cfg.getW() <= 0) ? 12 : cfg.getW();
        int h = (cfg.getH() == null || cfg.getH() <= 0) ? 8 : cfg.getH();
        String type = mapVisualization(cfg.getVisualization());

        Map<String, Object> gridPos = new HashMap<>();
        gridPos.put("x", x);
        gridPos.put("y", y);
        gridPos.put("w", w);
        gridPos.put("h", h);

        Map<String, Object> datasource = null;
        if (cfg.getDatasource() != null && !cfg.getDatasource().isBlank()) {
            datasource = Map.of("uid", cfg.getDatasource());
        }

        Map<String, Object> target = new HashMap<>();
        target.put("refId", "A");
        if (cfg.getQuery() != null) {
            // Assume Prometheus expr; for generic Grafana, this will vary.
            target.put("expr", cfg.getQuery());
        }
        if (datasource != null) target.put("datasource", datasource);

        List<Map<String, Object>> targets = List.of(target);

        Map<String, Object> panel = new LinkedHashMap<>();
        panel.put("id", id);
        panel.put("type", type);
        panel.put("title", cfg.getTitle() == null ? ("Panel " + id) : cfg.getTitle());
        panel.put("gridPos", gridPos);
        if (datasource != null) panel.put("datasource", datasource);
        panel.put("targets", targets);

        Map<String, Object> fieldConfig = buildFieldConfig(cfg.getUnit(), cfg.getThresholds());
        if (!fieldConfig.isEmpty()) panel.put("fieldConfig", fieldConfig);

        return panel;
    }

    private Map<String, Object> buildFieldConfig(String unit, String thresholds) {
        Map<String, Object> fieldConfig = new HashMap<>();
        Map<String, Object> defaults = new HashMap<>();

        if (unit != null && !unit.isBlank()) {
            defaults.put("unit", unit);
        }
        if (thresholds != null && !thresholds.isBlank()) {
            Map<String, Object> th = new HashMap<>();
            th.put("mode", "absolute");
            List<Map<String, Object>> steps = new ArrayList<>();
            steps.add(Map.of("color", "green", "value", null));
            String[] parts = thresholds.split("\\|");
            if (parts.length >= 1) {
                try { steps.add(Map.of("color", "yellow", "value", Double.parseDouble(parts[0]))); } catch (Exception ignored) {}
            }
            if (parts.length >= 2) {
                try { steps.add(Map.of("color", "red", "value", Double.parseDouble(parts[1]))); } catch (Exception ignored) {}
            }
            th.put("steps", steps);
            defaults.put("thresholds", th);
        }

        if (!defaults.isEmpty()) {
            fieldConfig.put("defaults", defaults);
            fieldConfig.put("overrides", List.of());
        }
        return fieldConfig;
    }

    private String mapVisualization(String vis) {
        if (vis == null) return "timeseries";
        switch (vis.toLowerCase(Locale.ROOT)) {
            case "stat": return "stat";
            case "barchart":
            case "bar": return "barchart";
            case "gauge": return "gauge";
            default: return "timeseries";
        }
    }
}
