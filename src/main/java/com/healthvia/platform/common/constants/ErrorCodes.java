// common/constants/ErrorCodes.java
package com.healthvia.platform.common.constants;
//
public enum ErrorCodes {
    // Authentication Errors (1000-1099)
    INVALID_CREDENTIALS("ERR_1000", "Geçersiz kullanıcı adı veya şifre"),
    TOKEN_EXPIRED("ERR_1001", "Token süresi dolmuş"),
    TOKEN_INVALID("ERR_1002", "Geçersiz token"),
    ACCOUNT_LOCKED("ERR_1003", "Hesap kilitlenmiş"),
    EMAIL_NOT_VERIFIED("ERR_1004", "Email doğrulanmamış"),
    UNAUTHORIZED("ERR_1005", "Yetkisiz erişim"),
    
    // User Errors (1100-1199)
    USER_NOT_FOUND("ERR_1100", "Kullanıcı bulunamadı"),
    USER_ALREADY_EXISTS("ERR_1101", "Kullanıcı zaten mevcut"),
    INVALID_USER_STATUS("ERR_1102", "Geçersiz kullanıcı durumu"),
    
    // Business Logic Errors (2000-2099)
    APPOINTMENT_NOT_AVAILABLE("ERR_2000", "Randevu müsait değil"),
    APPOINTMENT_ALREADY_BOOKED("ERR_2001", "Randevu zaten alınmış"),
    INVALID_APPOINTMENT_DATE("ERR_2002", "Geçersiz randevu tarihi"),
    PAYMENT_FAILED("ERR_2003", "Ödeme başarısız"),
    INSUFFICIENT_BALANCE("ERR_2004", "Yetersiz bakiye"),
    CANCELLATION_DEADLINE_PASSED("ERR_2005", "Randevu iptal süresi aşılmış"),
    
    // Integration Errors (3000-3099)
    ZOOM_API_ERROR("ERR_3000", "Zoom API hatası"),
    HOTEL_API_ERROR("ERR_3001", "Otel API hatası"),
    FLIGHT_API_ERROR("ERR_3002", "Uçuş API hatası"),
    PAYMENT_GATEWAY_ERROR("ERR_3003", "Ödeme gateway hatası"),
    
    // System Errors (9000-9099)
    INTERNAL_SERVER_ERROR("ERR_9000", "Sistem hatası"),
    DATABASE_ERROR("ERR_9001", "Veritabanı hatası"),
    FILE_UPLOAD_ERROR("ERR_9002", "Dosya yükleme hatası"),
    CACHE_ERROR("ERR_9003", "Önbellek hatası"),
    VALIDATION_ERROR("ERR_1200", "Validasyon hatası"),
    INVALID_TC_KIMLIK("ERR_1201", "Geçersiz TC Kimlik Numarası");
    
    private final String code;
    private final String message;
    
    ErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}
