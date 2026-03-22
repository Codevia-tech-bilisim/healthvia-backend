// common/dto/ApiResponse.java
package com.healthvia.platform.common.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private boolean success;
    private String message;
    private T data;
    private ErrorResponse error;
    private LocalDateTime timestamp;
    
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .message(message)
            .data(data)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
            .success(true)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
            .success(true)
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();
    }
    
    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return ApiResponse.<T>builder()
            .success(false)
            .message(error.getMessage())
            .error(error)
            .timestamp(LocalDateTime.now())
            .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        ErrorResponse error = ErrorResponse.builder()
            .message(message)
            .timestamp(LocalDateTime.now())
            .build();

        return ApiResponse.<T>builder()
            .success(false)
            .message(message)
            .error(error)
            .timestamp(LocalDateTime.now())
            .build();
    }
}