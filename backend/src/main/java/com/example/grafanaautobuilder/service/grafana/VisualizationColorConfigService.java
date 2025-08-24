package com.example.grafanaautobuilder.service.grafana;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class VisualizationColorConfigService {
    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, Object> config;

    public VisualizationColorConfigService() {
        load();
    }

    private void load() {
        try {
            ClassPathResource res = new ClassPathResource("visualization-colors.json");
            if (!res.exists()) {
                config = Collections.emptyMap();
                return;
            }
            try (InputStream is = res.getInputStream()) {
                this.config = mapper.readValue(is, new TypeReference<Map<String, Object>>(){});
            }
        } catch (IOException e) {
            // If parsing fails, default to empty to avoid breaking requests
            this.config = Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getFieldConfigDefaults() {
        Object fc = config.get("fieldConfig");
        if (!(fc instanceof Map)) return Collections.emptyMap();
        Object defs = ((Map<String, Object>) fc).get("defaults");
        if (defs instanceof Map) return (Map<String, Object>) defs;
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getFieldConfigOverrides() {
        Object fc = config.get("fieldConfig");
        if (!(fc instanceof Map)) return Collections.emptyList();
        Object overrides = ((Map<String, Object>) fc).get("overrides");
        if (overrides instanceof List) return (List<Map<String, Object>>) overrides;
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public List<String> getPalette() {
        Object pal = config.get("palette");
        if (pal instanceof List) return (List<String>) pal;
        return Collections.emptyList();
    }

    public String getTheme() {
        Object t = config.get("theme");
        return t == null ? null : String.valueOf(t);
    }
}
