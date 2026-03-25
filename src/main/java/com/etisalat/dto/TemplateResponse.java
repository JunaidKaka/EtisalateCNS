package com.etisalat.dto;

import com.etisalat.models.TemplateStatus;
import com.etisalat.models.TemplateType;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class TemplateResponse {
    private Long id;
    private TemplateType type;
    private long notid;
    private String content;
    private String title;
    private String subject;
    private String category;
    private TemplateStatus status;
    private String createdBy;
    private Instant createdAt;
    private String language;
    private String qaComments;
    private String senderId;
    private String shortcode;
    private String ppmID;
    private String deploymentstatus;
    private String systemName;
}
