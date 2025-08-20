package com.example.grafanaautobuilder.service.grafana;

import com.example.grafanaautobuilder.dto.PanelConfig;
import com.example.grafanaautobuilder.config.GrafanaProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class PanelJsonBuilder {
    private final GrafanaProperties grafanaProperties;
    private final VisualizationColorConfigService colorConfigService;

    public PanelJsonBuilder(GrafanaProperties grafanaProperties, VisualizationColorConfigService colorConfigService) {
        this.grafanaProperties = grafanaProperties;
        this.colorConfigService = colorConfigService;
    }

    public Map<String, Object> buildPanel(PanelConfig cfg, int x, int y, int id) {
        // 1) Load universal JSON template and replace placeholders for core inputs
        String rawTemplate = loadTemplate("panel-templates/universal-panel.json");
        if (rawTemplate == null || rawTemplate.isBlank()) {
            // Fallback to previous behavior if template missing
            return buildPanelFallback(cfg, x, y, id);
        }

        String type = mapVisualization(cfg.getVisualization());
        String title = (cfg.getTitle() == null || cfg.getTitle().isBlank()) ? ("Panel " + id) : cfg.getTitle();
        String query = cfg.getQuery() == null ? "" : cfg.getQuery();

        String filled = rawTemplate
                .replace("{{visualization}}", type)
                .replace("{{title}}", escapeJson(title))
                .replace("{{query}}", escapeJson(query));
        // We do NOT rely on the template's datasource placeholders; we'll inject programmatically

        Map<String, Object> panel = parseJsonToMap(filled);
        if (panel == null) {
            return buildPanelFallback(cfg, x, y, id);
        }

        // 2) Determine datasource UID and type with fallback to backend defaults
        String csvDatasource = cfg.getDatasource();
        String defaultUid = grafanaProperties.getDefaultDatasourceUid();
        String defaultType = grafanaProperties.getDefaultDatasourceType();

        Map<String, Object> datasource = null;
        if (csvDatasource != null && !csvDatasource.isBlank()) {
            datasource = new HashMap<>();
            datasource.put("uid", csvDatasource);
            if (defaultType != null && !defaultType.isBlank()) datasource.put("type", defaultType);
        } else if (defaultUid != null && !defaultUid.isBlank()) {
            datasource = new HashMap<>();
            datasource.put("uid", defaultUid);
            if (defaultType != null && !defaultType.isBlank()) datasource.put("type", defaultType);
        }

        if (datasource != null) {
            panel.put("datasource", datasource);
            Object targetsObj = panel.get("targets");
            if (targetsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> targets = (List<Object>) targetsObj;
                if (!targets.isEmpty() && targets.get(0) instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> t0 = (Map<String, Object>) targets.get(0);
                    t0.put("datasource", datasource);
                }
            }
        } else {
            // If neither CSV nor default, remove datasource blocks to let Grafana resolve a default
            panel.remove("datasource");
            Object targetsObj = panel.get("targets");
            if (targetsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> targets = (List<Object>) targetsObj;
                if (!targets.isEmpty() && targets.get(0) instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> t0 = (Map<String, Object>) targets.get(0);
                    t0.remove("datasource");
                }
            }
        }

        // 3) Replace template targets with a properly constructed target to avoid
        //    "Unrecognized query model format: auto" errors in Grafana (PostgreSQL).
        //    This mirrors the logic in buildPanelFallback and ensures rawSql/format are set.
        Map<String, Object> target = buildTarget(cfg, datasource);
        List<Map<String, Object>> targets = new ArrayList<>();
        targets.add(target);
        panel.put("targets", targets);

        // Also enforce core fields which may be stale in template
        panel.put("type", type);
        panel.put("title", title);

        // 4) Always set id, gridPos defaults, and transparent true.
        Map<String, Object> gridPos = new HashMap<>();
        gridPos.put("x", x);
        gridPos.put("y", y);
        // Keep reasonable defaults internally; W/H are not asked from user
        gridPos.put("w", 12);
        gridPos.put("h", 8);
        panel.put("id", id);
        panel.put("gridPos", gridPos);
        panel.put("transparent", true);

        // 5) Provide ECharts-specific options: customColor and gradient toggle.
        //    Color precedence: CSV color > palette pick by title.
        String chosenColor = cfg.getColor();
        if (chosenColor == null || chosenColor.isBlank()) {
            String seed = title != null ? title : ("Panel " + id);
            chosenColor = pickColorFromPalette(seed);
        }
        if ("volkovlabs-echarts-panel".equals(panel.get("type"))) {
            @SuppressWarnings("unchecked")
            Map<String, Object> options = (Map<String, Object>) panel.getOrDefault("options", new HashMap<>());
            if (chosenColor != null && !chosenColor.isBlank()) {
                options.put("customColor", chosenColor);
            }
            // Enable gradient by default for business/eCharts time series look
            options.put("useGradient", true);
            panel.put("options", options);
        }

        // 6) Inject fieldConfig merged with global visualization-colors.json even in template path
        Map<String, Object> fieldConfig = buildFieldConfig(cfg.getUnit(), cfg.getThresholds(), cfg.getVisualization(), chosenColor);
        Map<String, Object> mergedFieldConfig = mergeWithGlobalFieldConfig(fieldConfig);
        if (!mergedFieldConfig.isEmpty()) {
            panel.put("fieldConfig", mergedFieldConfig);
        }

        return panel;
    }

    // Pick a stable color from the configured palette based on a text seed (e.g., panel title)
    private String pickColorFromPalette(String seed) {
        List<String> palette = colorConfigService.getPalette();
        if (palette == null || palette.isEmpty()) return null;
        int hash = 0;
        if (seed != null) {
            for (int i = 0; i < seed.length(); i++) {
                hash = (31 * hash + seed.charAt(i));
            }
        }
        int idx = (hash & 0x7fffffff) % palette.size();
        return palette.get(idx);
    }

    private Map<String, Object> buildTarget(PanelConfig cfg, Map<String, Object> datasource) {
        Map<String, Object> target = new HashMap<>();
        target.put("refId", "A");
        
        if (cfg.getQuery() != null) {
            // Determine datasource type from configured default; CSV value is UID, not type
            String defaultType = grafanaProperties.getDefaultDatasourceType();
            // Project supports only PostgreSQL datasources by default; avoid accidental Prometheus
            String datasourceType = defaultType != null ? defaultType.toLowerCase() : "postgres";

            if (datasourceType.contains("postgres") || datasourceType.contains("postgresql")) {
                // PostgreSQL query format
                target.put("rawQuery", true);
                String sql = cfg.getQuery();
                target.put("rawSql", sql);

                // Heuristic: decide if query returns time series or category table
                boolean selectsTimeAlias = sql.toLowerCase(Locale.ROOT).contains(" as time")
                        || sql.toLowerCase(Locale.ROOT).contains("as \"time\"");
                boolean isBarVis = cfg.getVisualization() != null
                        && (cfg.getVisualization().equalsIgnoreCase("barchart")
                            || cfg.getVisualization().equalsIgnoreCase("bar")
                            || cfg.getVisualization().equalsIgnoreCase("echarts-bar"));
                boolean isEchartsLine = cfg.getVisualization() != null
                        && (cfg.getVisualization().equalsIgnoreCase("echarts-line")
                            || cfg.getVisualization().equalsIgnoreCase("echarts")
                            || cfg.getVisualization().equalsIgnoreCase("business"));
                boolean isTableVis = cfg.getVisualization() != null
                        && cfg.getVisualization().equalsIgnoreCase("table");
                boolean isStatVis = cfg.getVisualization() != null
                        && cfg.getVisualization().equalsIgnoreCase("stat");
                boolean isGaugeVis = cfg.getVisualization() != null
                        && cfg.getVisualization().equalsIgnoreCase("gauge");

                // Format selection rules to avoid "db has no time column" errors:
                // 1) Table viz -> table
                // 2) Stat/Gauge -> table unless query explicitly selects a time column
                // 3) Bar -> table unless query explicitly selects a time column
                // 4) ECharts line/business -> time_series if time exists, else table
                // 5) Otherwise -> time_series
                if (isTableVis) {
                    target.put("format", "table");
                } else if (isStatVis || isGaugeVis) {
                    target.put("format", selectsTimeAlias ? "time_series" : "table");
                } else if (isBarVis) {
                    target.put("format", selectsTimeAlias ? "time_series" : "table");
                } else if (isEchartsLine) {
                    target.put("format", selectsTimeAlias ? "time_series" : "table");
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

    // Legacy fallback builder (kept for safety if template missing or parse fails)
    private Map<String, Object> buildPanelFallback(PanelConfig cfg, int x, int y, int id) {
        int w = (cfg.getW() == null || cfg.getW() <= 0) ? 12 : cfg.getW();
        int h = (cfg.getH() == null || cfg.getH() <= 0) ? 8 : cfg.getH();
        String type = mapVisualization(cfg.getVisualization());

        Map<String, Object> gridPos = new HashMap<>();
        gridPos.put("x", x);
        gridPos.put("y", y);
        gridPos.put("w", w);
        gridPos.put("h", h);

        Map<String, Object> datasource = null;
        String csvDatasource = cfg.getDatasource();
        String defaultUid = grafanaProperties.getDefaultDatasourceUid();
        String defaultType = grafanaProperties.getDefaultDatasourceType();
        if (csvDatasource != null && !csvDatasource.isBlank()) {
            datasource = new HashMap<>();
            datasource.put("uid", csvDatasource);
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
        panel.put("transparent", true);
        if (datasource != null) panel.put("datasource", datasource);
        panel.put("targets", targets);

        // Keep previous options/fieldConfig heuristics for legacy behavior
        Map<String, Object> options = buildPanelOptions(cfg.getVisualization());
        if (!options.isEmpty()) panel.put("options", options);

        String chosenColor = cfg.getColor();
        if (chosenColor == null || chosenColor.isBlank()) {
            String seed = cfg.getTitle() != null ? cfg.getTitle() : ("Panel " + id);
            chosenColor = pickColorFromPalette(seed);
        }
        Map<String, Object> fieldConfig = buildFieldConfig(cfg.getUnit(), cfg.getThresholds(), cfg.getVisualization(), chosenColor);
        Map<String, Object> mergedFieldConfig = mergeWithGlobalFieldConfig(fieldConfig);
        if (!mergedFieldConfig.isEmpty()) panel.put("fieldConfig", mergedFieldConfig);

        return panel;
    }

    private String loadTemplate(String classpathLocation) {
        ClassPathResource resource = new ClassPathResource(classpathLocation);
        try (InputStream is = resource.getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonToMap(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        // Simple escape for quotes and backslashes used within template replacements
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private Map<String, Object> buildPanelOptions(String visualization) {
        Map<String, Object> options = new HashMap<>();
        
        if (visualization != null) {
            switch (visualization.toLowerCase(Locale.ROOT)) {
                case "stat":
                    Map<String, Object> statOptions = new HashMap<>();
                    statOptions.put("colorMode", "value");
                    statOptions.put("graphMode", "area"); // sparkline
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
                    
                case "table":
                    Map<String, Object> tableOptions = new HashMap<>();
                    // Minimal sensible defaults for table panels
                    tableOptions.put("showHeader", true);
                    options.putAll(tableOptions);
                    break;
                    
                case "barchart":
                case "bar":
                    Map<String, Object> barOptions = new HashMap<>();
                    // Vertical bars with value labels for a modern business look
                    barOptions.put("orientation", "vertical");
                    barOptions.put("showValue", "always");
                    barOptions.put("stacking", "none");
                    options.putAll(barOptions);
                    break;
                    
                case "gauge":
                    Map<String, Object> gaugeOptions = new HashMap<>();
                    gaugeOptions.put("orientation", "auto");
                    gaugeOptions.put("showThresholdLabels", false);
                    gaugeOptions.put("showThresholdMarkers", true);
                    gaugeOptions.put("reduceOptions", Map.of(
                        "calcs", List.of("lastNotNull"),
                        "fields", "",
                        "values", false
                    ));
                    options.putAll(gaugeOptions);
                    break;
                    
                case "echarts":
                case "echarts-line":
                case "echarts-bar":
                case "business":
                    // Provide a safe default ECharts script that auto-detects time vs category data
                    // and avoids undefined access when frames are empty or shapes differ.
                    String seriesType = (visualization != null && visualization.equalsIgnoreCase("echarts-bar")) ? "bar" : "line";
                    StringBuilder fn = new StringBuilder();
                    fn.append("// Auto ECharts function generated by Grafana Autobuilder\n")
                      .append("try {\n")
                      .append("const panel = (context && context.panel) ? context.panel : {};\n")
                      .append("const panelOpts = panel.options || context.options || {};\n")
                      .append("const fallbackColor = '#3B82F6';\n")
                      .append("const color = (panelOpts && panelOpts.customColor) ? panelOpts.customColor : fallbackColor;\n")
                      .append("function vectorToArray(v){ try { if (!v) return []; if (Array.isArray(v)) return v; if (typeof v.toArray==='function') return v.toArray(); if (typeof v.length==='number' && typeof v.get==='function'){ const out=[]; for(let i=0;i<v.length;i++){ out.push(v.get(i)); } return out; } return []; } catch(_) { return []; } }\n")
                      .append("function first(a){ return (Array.isArray(a) && a.length) ? a[0] : null; }\n")
                      .append("function hexToRgba(hex, alpha) {\n")
                      .append("  if (!hex) return 'rgba(59,130,246,' + (alpha ?? 1) + ')';\n")
                      .append("  const m = /^#?([a-f0-9]{2})([a-f0-9]{2})([a-f0-9]{2})$/i.exec(hex);\n")
                      .append("  if (!m) return 'rgba(59,130,246,' + (alpha ?? 1) + ')';\n")
                      .append("  const r = parseInt(m[1],16), g = parseInt(m[2],16), b = parseInt(m[3],16);\n")
                      .append("  return `rgba(${r}, ${g}, ${b}, ${alpha ?? 1})`;\n")
                      .append("}\n")
                      .append("function safeValues(field){ try { return field && ('values' in field) ? vectorToArray(field.values) : []; } catch(_) { return []; } }\n")
                      .append("function getFrames(pd){ if (!pd) return []; if (Array.isArray(pd.series)) return pd.series; if (Array.isArray(pd.frames)) return pd.frames; if (Array.isArray(pd)) return pd; return []; }\n")
                      .append("const pdata = (panel && panel.data) ? panel.data : (context && context.data ? context.data : null);\n")
                      .append("const frames = getFrames(pdata);\n")
                      .append("if (!frames.length) { return { title: { text: 'No data' }, series: [] }; }\n")
                      .append("const frame = first(frames);\n")
                      .append("let timeField = null, valueField = null, labelField = null;\n")
                      .append("if (frame && Array.isArray(frame.fields) && frame.fields.length){\n")
                      .append("  frame.fields.forEach(f => {\n")
                      .append("    const t = (f.type || '').toLowerCase();\n")
                      .append("    const n = (f.name || '').toLowerCase();\n")
                      .append("    if (!timeField && (t.includes('time') || n === 'time' || n === 'timestamp')) timeField = f;\n")
                      .append("    else if (!valueField && (t.includes('number') || t.includes('numeric'))) valueField = f;\n")
                      .append("    else if (!labelField) labelField = f;\n")
                      .append("  });\n")
                      .append("}\n")
                      .append("if (!valueField && frame && Array.isArray(frame.rows) && Array.isArray(frame.fields)){\n")
                      .append("  const vIdx = frame.fields.findIndex(c => ((c.type||'').toLowerCase().includes('number')));\n")
                      .append("  if (vIdx >= 0) valueField = { values: frame.rows.map(r => r[vIdx]) };\n")
                      .append("  const tIdx = frame.fields.findIndex(c => ((c.type||'').toLowerCase().includes('time') || (c.name||'').toLowerCase() === 'time'));\n")
                      .append("  if (tIdx >= 0) timeField = { values: frame.rows.map(r => r[tIdx]) };\n")
                      .append("  if (!timeField) labelField = { values: frame.rows.map(r => r[0]) };\n")
                      .append("}\n")
                      .append("if (!valueField){ return { title: { text: 'No numeric data' }, series: [] }; }\n")
                      .append("const isTime = !!timeField;\n")
                      .append("const labels = isTime ? safeValues(timeField) : safeValues(labelField);\n")
                      .append("const values = safeValues(valueField);\n")
                      .append("let seriesData;\n")
                      .append("if (isTime) { const tv = labels; const nv = values; const len = Math.min(tv.length, nv.length); seriesData = []; for (let i=0;i<len;i++) seriesData.push([tv[i], nv[i]]); } else { seriesData = values; }\n")
                      .append("const useGradient = !!panelOpts.useGradient;\n")
                      .append("const visualMap = useGradient ? { show: false, orient: 'horizontal', type: 'continuous', inRange: { color: ['#22C55E', '#EAB308', '#EF4444'] } } : undefined;\n")
                      .append("return { color: [color], tooltip: { trigger: isTime ? 'axis' : 'item' }, xAxis: { type: isTime ? 'time' : 'category', data: isTime ? undefined : labels }, yAxis: { type: 'value' }, visualMap, series: [ { type: '").append(seriesType).append("', showSymbol: false, smooth: true, itemStyle: { color }, areaStyle: isTime ? { color: { type: 'linear', x:0, y:0, x2:0, y2:1, colorStops: [ { offset: 0, color: hexToRgba(color, 0.28) }, { offset: 1, color: hexToRgba(color, 0.02) } ] } } : undefined, data: seriesData } ] };\n")
                      .append("} catch(e) { try { if (typeof console !== 'undefined') console.error('[Autobuilder ECharts Error]', e); } catch(_) {} return { title: { text: 'Data error' }, series: [] }; }\n");
                    options.put("getOptions", fn.toString());
                    options.put("useDataFrames", true);
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
                    // Visual improvements
                    timeSeriesOptions.put("drawStyle", "line");
                    timeSeriesOptions.put("lineInterpolation", "smooth");
                    timeSeriesOptions.put("lineWidth", 2);
                    timeSeriesOptions.put("fillOpacity", 12);
                    timeSeriesOptions.put("showPoints", "auto");
                    timeSeriesOptions.put("spanNulls", true);
                    options.putAll(timeSeriesOptions);
                    break;
            }
        }
        
        return options;
    }

    private Map<String, Object> buildFieldConfig(String unit, String thresholds, String visualization, String explicitColor) {
        Map<String, Object> fieldConfig = new HashMap<>();
        Map<String, Object> defaults = new HashMap<>();

        if (unit != null && !unit.isBlank()) {
            defaults.put("unit", unit);
        }
        // Heuristic decimals by unit
        if (unit != null) {
            String u = unit.toLowerCase(Locale.ROOT);
            if (u.contains("percent")) {
                defaults.put("decimals", 1);
            } else if (u.contains("currency")) {
                defaults.put("decimals", 0);
            } else {
                defaults.put("decimals", 2);
            }
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

        // Apply per-visualization default colors if none are specified via CSV (we don't have CSV colors yet)
        if (visualization != null) {
            String vis = visualization.toLowerCase(Locale.ROOT);
            Map<String, Object> color = new HashMap<>();
            switch (vis) {
                case "stat":
                    // Fixed modern blue for stat (or explicit override)
                    color.put("mode", "fixed");
                    color.put("fixedColor", (explicitColor != null && !explicitColor.isBlank()) ? explicitColor : "#60A5FA");
                    defaults.put("color", color);
                    break;
                case "gauge":
                    // Fixed modern green for gauge (or explicit override)
                    color.put("mode", "fixed");
                    color.put("fixedColor", (explicitColor != null && !explicitColor.isBlank()) ? explicitColor : "#22C55E");
                    defaults.put("color", color);
                    // If percent unit, clamp 0-100
                    if (unit != null && unit.toLowerCase(Locale.ROOT).contains("percent")) {
                        Map<String, Object> custom = (Map<String, Object>) defaults.getOrDefault("custom", new HashMap<>());
                        custom.put("min", 0);
                        custom.put("max", 100);
                        defaults.put("custom", custom);
                    }
                    break;
                case "barchart":
                case "bar":
                    // Fixed modern cyan for bar charts (or explicit override)
                    color.put("mode", "fixed");
                    color.put("fixedColor", (explicitColor != null && !explicitColor.isBlank()) ? explicitColor : "#06B6D4");
                    defaults.put("color", color);
                    break;
                default:
                    // Timeseries: allow explicit color, otherwise use palette
                    if (explicitColor != null && !explicitColor.isBlank()) {
                        color.put("mode", "fixed");
                        color.put("fixedColor", explicitColor);
                    } else {
                        color.put("mode", "palette-classic");
                    }
                    defaults.put("color", color);
                    // Suggest soft min for percentages
                    if (unit != null && unit.toLowerCase(Locale.ROOT).contains("percent")) {
                        Map<String, Object> custom = (Map<String, Object>) defaults.getOrDefault("custom", new HashMap<>());
                        custom.put("axisSoftMin", 0);
                        custom.put("axisSoftMax", 100);
                        defaults.put("custom", custom);
                    }
                    break;
            }
        }

        if (!defaults.isEmpty()) {
            fieldConfig.put("defaults", defaults);
            fieldConfig.put("overrides", new ArrayList<>());
        }
        return fieldConfig;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mergeWithGlobalFieldConfig(Map<String, Object> base) {
        if (base == null) base = new HashMap<>();
        Map<String, Object> out = new HashMap<>(base);

        // Merge defaults (do not override existing keys in base)
        Map<String, Object> baseDefaults = (Map<String, Object>) out.getOrDefault("defaults", new HashMap<>());
        Map<String, Object> globalDefaults = colorConfigService.getFieldConfigDefaults();
        if (globalDefaults != null && !globalDefaults.isEmpty()) {
            Map<String, Object> mergedDefaults = new HashMap<>(globalDefaults);
            // Base keys win
            mergedDefaults.putAll(baseDefaults);
            out.put("defaults", mergedDefaults);
        } else if (!baseDefaults.isEmpty()) {
            out.put("defaults", baseDefaults);
        }

        // Merge overrides: append global overrides after base overrides
        List<Map<String, Object>> baseOverrides = (List<Map<String, Object>>) out.getOrDefault("overrides", new ArrayList<>());
        List<Map<String, Object>> globalOverrides = colorConfigService.getFieldConfigOverrides();
        List<Map<String, Object>> mergedOverrides = new ArrayList<>();
        if (baseOverrides != null) mergedOverrides.addAll(baseOverrides);
        if (globalOverrides != null && !globalOverrides.isEmpty()) mergedOverrides.addAll(globalOverrides);
        if (!mergedOverrides.isEmpty()) out.put("overrides", mergedOverrides);

        return out;
    }

    private String mapVisualization(String vis) {
        if (vis == null) return "timeseries";
        switch (vis.toLowerCase(Locale.ROOT)) {
            case "stat": return "stat";
            case "table": return "table";
            case "barchart":
            case "bar": return "barchart";
            case "gauge": return "gauge";
            case "echarts":
            case "echarts-line":
            case "echarts-bar":
            case "business":
                return "volkovlabs-echarts-panel";
            default: return "timeseries";
        }
    }
}
