package com.example.grafanaautobuilder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "grafana")
public class GrafanaProperties {
    private String url;
    private String apiKey;
    private String folderUid;
    private Integer orgId;

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
}
