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
}
