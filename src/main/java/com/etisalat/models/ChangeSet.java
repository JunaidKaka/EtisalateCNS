package com.etisalat.models;

import java.util.LinkedHashMap;
import java.util.Map;

public class ChangeSet {

    private Map<String, FieldChange> fields = new LinkedHashMap<>();
    private String summary;
    private int count;

    public Map<String, FieldChange> getFields() { return fields; }
    public void setFields(Map<String, FieldChange> fields) { this.fields = fields; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public void addField(String name, String oldVal, String newVal) {
        fields.put(name, new FieldChange(oldVal, newVal));
        count = fields.size();
        summary = String.join(", ", fields.keySet());
    }

    public static class FieldChange {
        private String oldValue;
        private String newValue;

        public FieldChange() {}
        public FieldChange(String oldValue, String newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
        public String getOldValue() { return oldValue; }
        public void setOldValue(String oldValue) { this.oldValue = oldValue; }
        public String getNewValue() { return newValue; }
        public void setNewValue(String newValue) { this.newValue = newValue; }
    }
}