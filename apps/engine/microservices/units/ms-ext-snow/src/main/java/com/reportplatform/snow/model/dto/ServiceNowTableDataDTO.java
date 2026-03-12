package com.reportplatform.snow.model.dto;

import java.time.Instant;
import java.util.Map;

public class ServiceNowTableDataDTO {

    private String sysId;
    private Instant sysUpdatedOn;
    private Map<String, Object> fields;

    public ServiceNowTableDataDTO() {
    }

    public String getSysId() {
        return sysId;
    }

    public void setSysId(String sysId) {
        this.sysId = sysId;
    }

    public Instant getSysUpdatedOn() {
        return sysUpdatedOn;
    }

    public void setSysUpdatedOn(Instant sysUpdatedOn) {
        this.sysUpdatedOn = sysUpdatedOn;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }
}
