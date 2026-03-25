package com.etisalat.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiResponse<T>(
    Instant timestamp,
    int status,
    boolean success,
    String message,
    T data,
    List<ApiError> errors,
    Meta meta
) {}
