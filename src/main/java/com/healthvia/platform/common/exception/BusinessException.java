// common/exception/BusinessException.java
package com.healthvia.platform.common.exception;

import java.util.Map;

import com.healthvia.platform.common.constants.ErrorCodes;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    
    private final ErrorCodes errorCode;
    private final Map<String, String> details;
    
    // Constructor 1: Sadece ErrorCode
    public BusinessException(ErrorCodes errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = null;
    }
    
    // Constructor 2: ErrorCode + custom message
    public BusinessException(ErrorCodes errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.details = null;
    }
    
    // Constructor 3: ErrorCode + details Map
    public BusinessException(ErrorCodes errorCode, Map<String, String> details) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.details = details;
    }
    
    // Constructor 4: ErrorCode + custom message + details
    public BusinessException(ErrorCodes errorCode, String customMessage, Map<String, String> details) {
        super(customMessage);
        this.errorCode = errorCode;
        this.details = details;
    }
    
    // Constructor 5: ErrorCode + Throwable cause
    public BusinessException(ErrorCodes errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.details = null;
    }
}