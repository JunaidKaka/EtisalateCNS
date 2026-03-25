package com.etisalat.utils;

import com.etisalat.dto.ApiError;
import com.etisalat.dto.ApiResponse;
import com.etisalat.dto.Meta;

import java.time.Instant;
import java.util.List;

public final class ResponseBuilder {

  public static <T> ApiResponse<T> success(T data, String message) {
    return new ApiResponse<>(Instant.now(), 200, true, message, data, null, null);
  }
    public static <T> ApiResponse<T> success( String message) {
        return new ApiResponse<>(Instant.now(), 200, true, message, null, null, null);
    }

  public static <T> ApiResponse<T> withMeta(T data, Meta meta, String message) {
    return new ApiResponse<>(Instant.now(), 200, true, message, data, null, meta);
  }

  public static ApiResponse<Void> error(int status, String message, List<ApiError> errors) {
    return new ApiResponse<>(Instant.now(), status, false, message, null, errors, null);
  }
}
