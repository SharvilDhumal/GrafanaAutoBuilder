// Purpose: A Spring Boot configuration properties holder for all grafana.* settings from 
// application.yml
// (or env vars).

package com.example.grafanaautobuilder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "grafana")
public class GrafanaProperties {
    private String url;
    private String apiKey;
    private String folderUid;
    private Integer orgId;
    // Default datasource settings used when panel CSV does not specify one
    private String defaultDatasourceUid; // e.g., UID of 'grafana_autobuilder' datasource
    private String defaultDatasourceType; // e.g., "postgres", "prometheus"

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getFolderUid() {
        return folderUid;
    }

    public void setFolderUid(String folderUid) {
        this.folderUid = folderUid;
    }

    public Integer getOrgId() {
        return orgId;
    }

    public void setOrgId(Integer orgId) {
        this.orgId = orgId;
    }

    public String getDefaultDatasourceUid() {
        return defaultDatasourceUid;
    }

    public void setDefaultDatasourceUid(String defaultDatasourceUid) {
        this.defaultDatasourceUid = defaultDatasourceUid;
    }

    public String getDefaultDatasourceType() {
        return defaultDatasourceType;
    }

    public void setDefaultDatasourceType(String defaultDatasourceType) {
        this.defaultDatasourceType = defaultDatasourceType;
    }
}
