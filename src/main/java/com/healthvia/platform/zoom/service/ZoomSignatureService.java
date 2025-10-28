package com.healthvia.platform.zoom.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class ZoomSignatureService {

    @Value("${zoom.sdk.key}")
    private String sdkKey;

    @Value("${zoom.sdk.secret}")
    private String sdkSecret;

    public String getSdkKey() {
        return this.sdkKey;
    }

    public String generateSignature(String meetingNumber, int role) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        // İmzanın geçerlilik süresi (örnek: 2 saat)
        long expMillis = nowMillis + (2 * 60 * 60 * 1000);
        Date exp = new Date(expMillis);

        SecretKey key = Keys.hmacShaKeyFor(sdkSecret.getBytes());

        return Jwts.builder()
            .setHeaderParam("typ", "JWT")
            .claim("appKey", sdkKey)
            .claim("sdkKey", sdkKey)
            .claim("mn", meetingNumber)
            .claim("role", role)
            .claim("iat", now.getTime() / 1000)
            .claim("exp", exp.getTime() / 1000)
            .claim("tokenExp", exp.getTime() / 1000)
            .signWith(key)
            .compact();
    }
}