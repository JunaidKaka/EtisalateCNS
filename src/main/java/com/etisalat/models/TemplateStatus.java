package com.etisalat.models;

public enum TemplateStatus {
    PENDING_VERIFICATION,   // after create by Dev
    DEV_VERIFIED,           // after Dev clicks "Verify"
    QA_REJECTED,            // QA rejected -> back to dev (editable)
    QA_VERIFIED
}