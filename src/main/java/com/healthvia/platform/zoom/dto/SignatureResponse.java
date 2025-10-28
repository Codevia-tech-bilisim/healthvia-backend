package com.healthvia.platform.zoom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignatureResponse {
    private String signature;
    private String sdkKey;
}