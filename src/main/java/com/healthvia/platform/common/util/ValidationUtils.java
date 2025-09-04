package com.healthvia.platform.common.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

public class ValidationUtils {
    
    private static final Pattern TURKISH_NAME_PATTERN = 
        Pattern.compile("^[a-zA-ZğüşıöçĞÜŞİÖÇ\\s-']+$");
    
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^\\+90[0-9]{10}$");
    
    /**
     * URL'den gelen Turkish character'lı parametreleri decode et
     */
    public static String decodeUrlParameter(String parameter) {
        if (!StringUtils.hasText(parameter)) {
            return parameter;
        }
        
        try {
            return URLDecoder.decode(parameter, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // UTF-8 her zaman desteklenir, bu Exception normalde olmaz
            return parameter;
        }
    }
    
    /**
     * Turkish character içeren isim doğrulaması
     */
    public static boolean isValidTurkishName(String name) {
        return StringUtils.hasText(name) && 
               name.length() >= 2 && 
               name.length() <= 50 && 
               TURKISH_NAME_PATTERN.matcher(name).matches();
    }
    
    /**
     * Turkish phone number validation
     */
    public static boolean isValidTurkishPhone(String phone) {
        return StringUtils.hasText(phone) && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * Chief complaint validation (Turkish characters allowed)
     */
    public static String sanitizeChiefComplaint(String complaint) {
        if (!StringUtils.hasText(complaint)) {
            return complaint;
        }
        
        // URL decode işlemi
        String decoded = decodeUrlParameter(complaint);
        
        // XSS önleme - basit HTML tag temizlemesi
        decoded = decoded.replaceAll("<[^>]*>", "");
        
        // Maksimum uzunluk kontrolü
        if (decoded.length() > 500) {
            decoded = decoded.substring(0, 500);
        }
        
        return decoded.trim();
    }
}

