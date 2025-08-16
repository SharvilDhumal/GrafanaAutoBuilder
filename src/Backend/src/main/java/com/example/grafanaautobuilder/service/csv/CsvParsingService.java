package com.example.grafanaautobuilder.service.csv;

import com.example.grafanaautobuilder.dto.PanelConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvParsingService {
    private static final Logger log = LoggerFactory.getLogger(CsvParsingService.class);

    public List<PanelConfig> parse(InputStream inputStream) throws IOException {
        long start = System.currentTimeMillis();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {
            List<PanelConfig> list = new ArrayList<>();
            for (CSVRecord r : parser) {
                PanelConfig cfg = new PanelConfig();
                cfg.setTitle(get(r, "title"));
                cfg.setDatasource(get(r, "datasource"));
                cfg.setQuery(get(r, "query"));
                // Support multiple header variants mapping to visualization:
                // visualization (preferred), panel_type, chart_type, viewType
                String vis = firstNonBlank(
                        get(r, "visualization"),
                        get(r, "panel_type"),
                        get(r, "chart_type"),
                        get(r, "viewType")
                );
                cfg.setVisualization(vis);
                cfg.setUnit(get(r, "unit"));
                cfg.setThresholds(get(r, "thresholds"));
                cfg.setW(parseIntOrNull(get(r, "w")));
                cfg.setH(parseIntOrNull(get(r, "h")));
                // Optional per-panel time overrides
                cfg.setTimeFrom(get(r, "timeFrom"));
                cfg.setTimeShift(get(r, "timeShift"));
                list.add(cfg);
            }
            log.info("CSV parsed: {} records in {} ms", list.size(), (System.currentTimeMillis() - start));
            return list;
        }
    }

    private static Integer parseIntOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static String get(CSVRecord r, String name) {
        try { return r.isMapped(name) ? r.get(name) : null; } catch (Exception e) { return null; }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null) {
                String t = v.trim();
                if (!t.isBlank()) return t;
            }
        }
        return null;
    }
}
