package com.healthvia.platform.zoom.controller;

import org.springframework.web.bind.annotation.*;

import com.healthvia.platform.zoom.service.ZoomSignatureService;

@RestController
@RequestMapping("/api/zoom")
@CrossOrigin // Frontend'in farklı bir adresten (localhost:3000 gibi) istek atabilmesi için
public class ZoomController {

    private final ZoomSignatureService zoomSignatureService;

    public ZoomController(ZoomSignatureService zoomSignatureService) {
        this.zoomSignatureService = zoomSignatureService;
    }

    // Frontend'den gelecek JSON'ın formatı
    public static class SignatureRequest {
        public String meetingNumber;
        public int role;
    }

    // Bizim frontend'e döneceğimiz JSON'ın formatı
    public static class SignatureResponse {
        public String signature;
        public String sdkKey;
        
        public SignatureResponse(String signature, String sdkKey) {
            this.signature = signature;
            this.sdkKey = sdkKey;
        }
    }

    @PostMapping("/signature")
    public SignatureResponse getSignature(@RequestBody SignatureRequest request) {
        String signature = zoomSignatureService.generateSignature(
            request.meetingNumber,
            request.role
        );
        String sdkKey = zoomSignatureService.getSdkKey();

        return new SignatureResponse(signature, sdkKey);
    }
}