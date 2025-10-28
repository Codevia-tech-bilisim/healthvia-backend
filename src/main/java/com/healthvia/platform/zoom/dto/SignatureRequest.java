package com.healthvia.platform.zoom.dto;

import lombok.Data;
@Data
public class SignatureRequest {
    private String meetingNumber;
    private int role;
}