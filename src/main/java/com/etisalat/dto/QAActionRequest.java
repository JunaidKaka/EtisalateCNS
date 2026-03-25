package com.etisalat.dto;

import lombok.Data;

@Data
public class QAActionRequest {
    private String comments; // optional for accept, recommended for reject
}