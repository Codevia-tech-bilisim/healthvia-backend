package com.healthvia.platform.zoom.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception for Zoom API related errors
 */
public class ZoomApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ZoomApiException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = "ZOOM_API_ERROR";
    }

    public ZoomApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = "ZOOM_API_ERROR";
    }

    public ZoomApiException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public ZoomApiException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.errorCode = "ZOOM_API_ERROR";
    }

    public ZoomApiException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = "ZOOM_API_ERROR";
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
