// CreateTemplateRequest.java
package com.etisalat.dto;
import com.etisalat.models.TemplateType;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateTemplateRequest {
    @NotNull
    private TemplateType type;

    @NotNull
    private String content;

    private long notid;
    private String code;
    private String sname;
    private String language;
    private String senderId;
    private String shortCode;
    private String ppmId;
    private String deploymentStatus;
    private String title;
    private String shortDesc;
    private String linkUrl;


    private String subject;
    private String category;
}
