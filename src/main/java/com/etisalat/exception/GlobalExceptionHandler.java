package com.etisalat.exception;

import com.etisalat.dto.ApiError;
import com.etisalat.dto.ApiResponse;
import com.etisalat.utils.ResponseBuilder;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
    List<ApiError> errors = ex.getBindingResult().getFieldErrors().stream()
        .map(e -> new ApiError("VALIDATION_ERROR", e.getField(), e.getDefaultMessage()))
        .toList();
    var resp = ResponseBuilder.error(400, "Validation failed", errors);
    ex.printStackTrace();
    return ResponseEntity.badRequest().body(resp);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException ex, HttpServletRequest req) {
    var err = List.of(new ApiError("NOT_FOUND", null, ex.getMessage()));
    var resp = ResponseBuilder.error(404, "Resource not found", err);
    ex.printStackTrace();
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resp);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex, HttpServletRequest req) {
    var err = List.of(new ApiError("INTERNAL_ERROR", null, ex.getMessage()));
    var resp = ResponseBuilder.error(500, ex.getMessage(), err);
    ex.printStackTrace();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
  }
}
