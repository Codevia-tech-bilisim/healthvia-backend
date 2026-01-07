package com.healthvia.platform.zoom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Zoom OAuth token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZoomTokenResponse {

    /**
     * Access token for API calls
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * Token type (always "bearer")
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * Token expiration time in seconds (usually 3600 = 1 hour)
     */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /**
     * Scope of the token
     */
    private String scope;
}
