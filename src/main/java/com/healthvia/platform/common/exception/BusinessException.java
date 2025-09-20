// common/exception/BusinessException.java
package com.healthvia.platform.common.exception;

import com.healthvia.platform.common.constants.ErrorCodes;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    
    private final ErrorCodes errorCode;
    private final Object[] args;
    
    public BusinessException(ErrorCodes errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.args = null;
    }
    
    public BusinessException(ErrorCodes errorCode, Object... args) {
        super(String.format(errorCode.getMessage(), args));
        this.errorCode = errorCode;
        this.args = args;
    }
    
    public BusinessException(ErrorCodes errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
        this.args = null;
    }
     public ErrorCodes getErrorCode() {
        return errorCode;
    }
}