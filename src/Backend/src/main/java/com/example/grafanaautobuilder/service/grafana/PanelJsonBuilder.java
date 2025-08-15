package com.example.grafanaautobuilder.service.grafana;

import com.example.grafanaautobuilder.dto.PanelConfig;
import com.example.grafanaautobuilder.config.GrafanaProperties;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class PanelJsonBuilder {
    private final GrafanaProperties grafanaProperties;

    public PanelJsonBuilder(GrafanaProperties grafanaProperties) {
        this.grafanaProperties = grafanaProperties;
    }

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
        // Prefer CSV-provided datasource UID; else fallback to configured default
        String csvDatasource = cfg.getDatasource();
        String defaultUid = grafanaProperties.getDefaultDatasourceUid();
        String defaultType = grafanaProperties.getDefaultDatasourceType();
        if (csvDatasource != null && !csvDatasource.isBlank()) {
            datasource = new HashMap<>();
            datasource.put("uid", csvDatasource);
            // We don't know CSV's type; assume configured default to ensure Grafana treats it as SQL
            if (defaultType != null && !defaultType.isBlank()) {
                datasource.put("type", defaultType);
            }
        } else if (defaultUid != null && !defaultUid.isBlank()) {
            datasource = new HashMap<>();
            datasource.put("uid", defaultUid);
            if (defaultType != null && !defaultType.isBlank()) {
                datasource.put("type", defaultType);
            }
        }

        Map<String, Object> target = buildTarget(cfg, datasource);

        List<Map<String, Object>> targets = new ArrayList<>();
        targets.add(target);

        Map<String, Object> panel = new LinkedHashMap<>();
        panel.put("id", id);
        panel.put("type", type);
        panel.put("title", cfg.getTitle() == null ? ("Panel " + id) : cfg.getTitle());
        panel.put("gridPos", gridPos);
        if (datasource != null) panel.put("datasource", datasource);
        // Ensure targets are set on the panel
        panel.put("targets", targets);

        // Add time range configuration to fix "no data" issue
        Map<String, Object> options = buildPanelOptions(cfg.getVisualization());
        if (!options.isEmpty()) {
            panel.put("options", options);
        }

        Map<String, Object> fieldConfig = buildFieldConfig(cfg.getUnit(), cfg.getThresholds());
        if (!fieldConfig.isEmpty()) panel.put("fieldConfig", fieldConfig);

        return panel;
    }

    private Map<String, Object> buildTarget(PanelConfig cfg, Map<String, Object> datasource) {
        Map<String, Object> target = new HashMap<>();
        target.put("refId", "A");
        
        if (cfg.getQuery() != null) {
            // Determine datasource type from configured default; CSV value is UID, not type
            String defaultType = grafanaProperties.getDefaultDatasourceType();
            String datasourceType = defaultType != null ? defaultType.toLowerCase() : "prometheus";

            if (datasourceType.contains("postgres") || datasourceType.contains("postgresql")) {
                // PostgreSQL query format
                target.put("rawQuery", true);
                String sql = cfg.getQuery();
                target.put("rawSql", sql);

                // Heuristic: decide if query returns time series or category table
                boolean mentionsTimeMacros = sql.toLowerCase(Locale.ROOT).contains("$__timefilter")
                        || sql.toLowerCase(Locale.ROOT).contains("$__timefrom()")
                        || sql.toLowerCase(Locale.ROOT).contains("$__timeto()");
                boolean selectsTimeAlias = sql.toLowerCase(Locale.ROOT).contains(" as time")
                        || sql.toLowerCase(Locale.ROOT).contains("as \"time\"");
                boolean isBarVis = cfg.getVisualization() != null
                        && (cfg.getVisualization().equalsIgnoreCase("barchart")
                            || cfg.getVisualization().equalsIgnoreCase("bar"));

                // For barchart panels without time, use table format so categories render
                if (isBarVis && !(mentionsTimeMacros || selectsTimeAlias)) {
                    target.put("format", "table");
                } else {
                    target.put("format", "time_series");
                }
            } else {
                // Prometheus query format
                target.put("expr", cfg.getQuery());
            }
        }
        
        if (datasource != null) target.put("datasource", datasource);
        
        return target;
    }

    private Map<String, Object> buildPanelOptions(String visualization) {
        Map<String, Object> options = new HashMap<>();
        
        if (visualization != null) {
            switch (visualization.toLowerCase(Locale.ROOT)) {
                case "stat":
                    Map<String, Object> statOptions = new HashMap<>();
                    statOptions.put("colorMode", "value");
                    statOptions.put("graphMode", "area");
                    statOptions.put("justifyMode", "auto");
                    statOptions.put("orientation", "auto");
                    statOptions.put("reduceOptions", Map.of(
                        "calcs", List.of("lastNotNull"),
                        "fields", "",
                        "values", false
                    ));
                    statOptions.put("textMode", "auto");
                    options.putAll(statOptions);
                    break;
                    
                case "barchart":
                case "bar":
                    Map<String, Object> barOptions = new HashMap<>();
                    barOptions.put("orientation", "auto");
                    barOptions.put("showValue", "auto");
                    barOptions.put("stacking", "none");
                    options.putAll(barOptions);
                    break;
                    
                case "gauge":
                    Map<String, Object> gaugeOptions = new HashMap<>();
                    gaugeOptions.put("orientation", "auto");
                    gaugeOptions.put("showThresholdLabels", false);
                    gaugeOptions.put("showThresholdMarkers", true);
                    options.putAll(gaugeOptions);
                    break;
                    
                default: // timeseries
                    Map<String, Object> timeSeriesOptions = new HashMap<>();
                    timeSeriesOptions.put("legend", Map.of(
                        "calcs", List.of(),
                        "displayMode", "list",
                        "placement", "bottom"
                    ));
                    timeSeriesOptions.put("tooltip", Map.of(
                        "mode", "single",
                        "sort", "none"
                    ));
                    options.putAll(timeSeriesOptions);
                    break;
            }
        }
        
        return options;
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
            Map<String, Object> greenStep = new HashMap<>();
            greenStep.put("color", "green");
            greenStep.put("value", null);
            steps.add(greenStep);
            
            String[] parts = thresholds.split("\\|");
            if (parts.length >= 1) {
                try { 
                    Map<String, Object> yellowStep = new HashMap<>();
                    yellowStep.put("color", "yellow");
                    yellowStep.put("value", Double.parseDouble(parts[0]));
                    steps.add(yellowStep);
                } catch (Exception ignored) {}
            }
            if (parts.length >= 2) {
                try { 
                    Map<String, Object> redStep = new HashMap<>();
                    redStep.put("color", "red");
                    redStep.put("value", Double.parseDouble(parts[1]));
                    steps.add(redStep);
                } catch (Exception ignored) {}
            }
            th.put("steps", steps);
            defaults.put("thresholds", th);
        }

        if (!defaults.isEmpty()) {
            fieldConfig.put("defaults", defaults);
            fieldConfig.put("overrides", new ArrayList<>());
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
