package com.example.grafanaautobuilder.dto;

public class PanelConfig {
    private String title;
    private String datasource;
    private String query;
    private String visualization; // timeseries, stat, barchart
    private String unit; // e.g., percent, bytes, s
    private String thresholds; // e.g., "80|90"
    private Integer w; // width in grid units
    private Integer h; // height in grid units
    // Optional per-panel time range override (Grafana format: 24h, 7d, 30m, etc.)
    private String timeFrom;
    // Optional per-panel time shift override (e.g., 1h)
    private String timeShift;
    // Optional explicit color hex (e.g., #7C3AED). If set, overrides defaults.
    private String color;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getVisualization() {
        return visualization;
    }

    public void setVisualization(String visualization) {
        this.visualization = visualization;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getThresholds() {
        return thresholds;
    }

    public void setThresholds(String thresholds) {
        this.thresholds = thresholds;
    }

    public Integer getW() {
        return w;
    }

    public void setW(Integer w) {
        this.w = w;
    }

    public Integer getH() {
        return h;
    }

    public void setH(Integer h) {
        this.h = h;
    }

    public String getTimeFrom() {
        return timeFrom;
    }

    public void setTimeFrom(String timeFrom) {
        this.timeFrom = timeFrom;
    }

    public String getTimeShift() {
        return timeShift;
    }

    public void setTimeShift(String timeShift) {
        this.timeShift = timeShift;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
