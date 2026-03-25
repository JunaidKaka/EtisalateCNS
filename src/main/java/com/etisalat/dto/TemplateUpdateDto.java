package com.etisalat.dto;

public record TemplateUpdateDto(
    String code,
    String content,
    String title,
    String subject,
    String shortDesc,
    String language,
    String senderId,
    String shortCode,
    String ppmId,
    String deploymentStatus,
    String category,
    Boolean editable,
    String qaComments
) {}