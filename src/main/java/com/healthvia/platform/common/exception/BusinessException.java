// src/main/java/com/healthvia/platform/common/exception/BusinessException.java
package com.healthvia.platform.common.exception;

import java.util.Map;

import com.healthvia.platform.common.constants.ErrorCodes;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    
    private final ErrorCodes errorCode;
    private final Object[] args;
    private final Map<String, String> details;
    
    public BusinessException(ErrorCodes errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = null;
        this.details = null;
    }
    
    public BusinessException(ErrorCodes errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
        this.args = args;
        this.details = null;
    }
    
    // Yeni constructor - Map<String, String> details için
    public BusinessException(ErrorCodes errorCode, Map<String, String> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = null;
        this.details = details;
    }
    
    public BusinessException(ErrorCodes errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.args = null;
        this.details = null;
    }
    
    // Helper method to get message from details if available
    @Override
    public String getMessage() {
        if (details != null && details.containsKey("message")) {
            return details.get("message");
        }
        return super.getMessage();
    }
}