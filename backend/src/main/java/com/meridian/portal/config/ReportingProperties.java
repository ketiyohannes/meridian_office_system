package com.meridian.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "meridian.reporting")
public class ReportingProperties {

    private String sharedFolder = "/tmp/meridian-report-packages";
    private int retentionDays = 90;

    public String getSharedFolder() {
        return sharedFolder;
    }

    public void setSharedFolder(String sharedFolder) {
        this.sharedFolder = sharedFolder;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }
}
