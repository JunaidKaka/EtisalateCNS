package com.etisalat.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FilterDescriptor {
    private Object value;
    private String matchMode;

    public FilterDescriptor() {}
    public FilterDescriptor(Object value, String matchMode) {
        this.value = value; this.matchMode = matchMode;
    }

    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }

    public String getMatchMode() { return matchMode; }
    public void setMatchMode(String matchMode) { this.matchMode = matchMode; }
}
