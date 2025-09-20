package com.healthvia.platform.common.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    private String code;
    private String message;
    private Map<String, Object> details; // ✅ Bu tip doğru Map<String, Object>
    private LocalDateTime timestamp;
    private String path;
    private String correlationId; // ✅ Bu field eklendi
    
    // Static factory methods for convenience
    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse of(String code, String message, String correlationId) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .correlationId(correlationId)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse of(String code, String message, String correlationId, String path) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .correlationId(correlationId)
                .path(path)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    public static ErrorResponse withDetails(String code, String message, Map<String, Object> details) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
}