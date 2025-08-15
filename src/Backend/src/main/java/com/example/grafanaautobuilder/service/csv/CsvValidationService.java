package com.example.grafanaautobuilder.service.csv;

import com.example.grafanaautobuilder.dto.PanelConfig;
import com.example.grafanaautobuilder.dto.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CsvValidationService {
    private static final Logger log = LoggerFactory.getLogger(CsvValidationService.class);

    private final CsvParsingService csvParsingService;
    private final JdbcTemplate jdbcTemplate;

    public CsvValidationService(CsvParsingService csvParsingService, JdbcTemplate jdbcTemplate) {
        this.csvParsingService = csvParsingService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ValidationResult> validate(MultipartFile csvFile) throws IOException {
        List<PanelConfig> rows = csvParsingService.parse(csvFile.getInputStream());
        List<ValidationResult> results = new ArrayList<>();

        // Use a default time window: now-7d .. now
        long to = Instant.now().toEpochMilli();
        long from = Instant.now().minusSeconds(7 * 24 * 3600L).toEpochMilli();

        for (int i = 0; i < rows.size(); i++) {
            PanelConfig cfg = rows.get(i);
            String title = cfg.getTitle() != null ? cfg.getTitle() : ("Row " + (i + 1));
            String sql = cfg.getQuery();

            if (sql == null || sql.isBlank()) {
                results.add(ValidationResult.error(i + 1, title, "Empty query"));
                continue;
            }

            try {
                String prepared = prepareSql(sql, from, to);
                // Wrap with subselect to safely apply LIMIT 1 regardless of original query
                String wrapped = "SELECT * FROM (" + prepared + ") AS t LIMIT 1";
                jdbcTemplate.queryForList(wrapped);
                results.add(ValidationResult.ok(i + 1, title));
            } catch (Exception ex) {
                String msg = ex.getMessage();
                results.add(ValidationResult.error(i + 1, title, msg));
            }
        }
        log.info("CSV validation completed: {} rows", results.size());
        return results;
    }

    private static String prepareSql(String sql, long fromMs, long toMs) {
        // Replace Grafana macros with timestamps suitable for Postgres comparisons
        // $__timeFrom() / $__timeTo() usually compare against TIMESTAMP columns
        String isoFrom = toIso(fromMs);
        String isoTo = toIso(toMs);
        String s = sql;
        s = s.replaceAll("(?i)\\$__timefrom\\(\\)", "TIMESTAMP '" + isoFrom + "'");
        s = s.replaceAll("(?i)\\$__timeto\\(\\)", "TIMESTAMP '" + isoTo + "'");
        // $__timeFilter(col) -> col BETWEEN from AND to using explicit matcher to avoid escaping pitfalls
        Pattern p = Pattern.compile("(?i)\\$__timefilter\\(\\s*([^\\)]+)\\s*\\)");
        Matcher m = p.matcher(s);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String col = m.group(1);
            String rep = col + " BETWEEN TIMESTAMP '" + isoFrom + "' AND TIMESTAMP '" + isoTo + "'";
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        s = sb.toString();

        // Trim and remove trailing semicolon to allow subselect wrapping
        s = s.trim();
        if (s.endsWith(";")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static String toIso(long epochMs) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withLocale(Locale.ROOT)
                .withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochMilli(epochMs));
    }
}
