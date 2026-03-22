package com.healthvia.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "iyzico")
public class IyzicoProperties {
    private String apiKey;
    private String secretKey;
    private String baseUrl = "https://sandbox-api.iyzipay.com";
}
