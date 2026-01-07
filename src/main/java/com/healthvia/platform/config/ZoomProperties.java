package com.healthvia.platform.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

/**
 * Zoom Server-to-Server OAuth Configuration Properties
 *
 * Required environment variables:
 * - ZOOM_CLIENT_ID: Zoom App Client ID
 * - ZOOM_CLIENT_SECRET: Zoom App Client Secret
 * - ZOOM_ACCOUNT_ID: Zoom Account ID for Server-to-Server OAuth
 * - ZOOM_WEBHOOK_SECRET: (Optional) Webhook verification secret
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "zoom")
@Validated
public class ZoomProperties {

    /**
     * Zoom App Client ID
     */
    @NotBlank(message = "Zoom Client ID is required")
    private String clientId;

    /**
     * Zoom App Client Secret
     */
    @NotBlank(message = "Zoom Client Secret is required")
    private String clientSecret;

    /**
     * Zoom Account ID for Server-to-Server OAuth
     */
    @NotBlank(message = "Zoom Account ID is required")
    private String accountId;

    /**
     * Webhook verification secret (optional)
     */
    private String webhookSecret;

    /**
     * Zoom API Base URL
     */
    private String baseUrl = "https://api.zoom.us/v2";

    /**
     * Zoom OAuth Token URL
     */
    private String oauthUrl = "https://zoom.us/oauth/token";
}
