// auth/security/JwtTokenProvider.java
package com.healthvia.platform.auth.security;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.healthvia.platform.common.enums.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtTokenProvider {
    
    private final JwtProperties jwtProperties;
    private Key key;
    
    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }
    
    public String generateAccessToken(UserPrincipal userPrincipal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userPrincipal.getRole().name());
        claims.put("email", userPrincipal.getEmail());
        claims.put("fullName", userPrincipal.getFullName());
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userPrincipal.getId())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    public String generateRefreshToken(String userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());
        
        return Jwts.builder()
                .setSubject(userId)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
    
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.getSubject();
    }
    
    // ✅ YENİ - Role extraction metodu
    public UserRole getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        String roleString = claims.get("role", String.class);
        return UserRole.valueOf(roleString);
    }
    
    // ✅ YENİ - Email extraction metodu
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.get("email", String.class);
    }
    
    // ✅ YENİ - Full name extraction metodu
    public String getFullNameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        
        return claims.get("fullName", String.class);
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token");
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty");
        }
        return false;
    }
}

// // auth/security/JwtTokenProvider.java
// package com.healthvia.platform.auth.security;

// import java.security.Key;
// import java.util.Date;
// import java.util.HashMap;
// import java.util.Map;

// import org.springframework.stereotype.Component;

// import io.jsonwebtoken.Claims;
// import io.jsonwebtoken.ExpiredJwtException;
// import io.jsonwebtoken.Jwts;
// import io.jsonwebtoken.MalformedJwtException;
// import io.jsonwebtoken.SignatureAlgorithm;
// import io.jsonwebtoken.UnsupportedJwtException;
// import io.jsonwebtoken.io.Decoders;
// import io.jsonwebtoken.security.Keys;
// import jakarta.annotation.PostConstruct;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;

// @Component
// @Slf4j
// @RequiredArgsConstructor
// public class JwtTokenProvider {
    
//     private final JwtProperties jwtProperties;
//     private Key key;
    
//     @PostConstruct
//     public void init() {
//         byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
//         this.key = Keys.hmacShaKeyFor(keyBytes);
//     }
    
//     public String generateAccessToken(UserPrincipal userPrincipal) {
//         Date now = new Date();
//         Date expiryDate = new Date(now.getTime() + jwtProperties.getAccessTokenExpiration());
        
//         Map<String, Object> claims = new HashMap<>();
//         claims.put("role", userPrincipal.getRole().name());
//         claims.put("email", userPrincipal.getEmail());
//         claims.put("fullName", userPrincipal.getFullName());
        
//         return Jwts.builder()
//                 .setClaims(claims)
//                 .setSubject(userPrincipal.getId())
//                 .setIssuedAt(now)
//                 .setExpiration(expiryDate)
//                 .signWith(key, SignatureAlgorithm.HS512)
//                 .compact();
//     }
    
//     public String generateRefreshToken(String userId) {
//         Date now = new Date();
//         Date expiryDate = new Date(now.getTime() + jwtProperties.getRefreshTokenExpiration());
        
//         return Jwts.builder()
//                 .setSubject(userId)
//                 .setIssuedAt(now)
//                 .setExpiration(expiryDate)
//                 .signWith(key, SignatureAlgorithm.HS512)
//                 .compact();
//     }
    
//     public String getUserIdFromToken(String token) {
//         Claims claims = Jwts.parserBuilder()
//                 .setSigningKey(key)
//                 .build()
//                 .parseClaimsJws(token)
//                 .getBody();
        
//         return claims.getSubject();
//     }
    
//     public boolean validateToken(String token) {
//         try {
//             Jwts.parserBuilder()
//                 .setSigningKey(key)
//                 .build()
//                 .parseClaimsJws(token);
//             return true;
//         } catch (MalformedJwtException ex) {
//             log.error("Invalid JWT token");
//         } catch (ExpiredJwtException ex) {
//             log.error("Expired JWT token");
//         } catch (UnsupportedJwtException ex) {
//             log.error("Unsupported JWT token");
//         } catch (IllegalArgumentException ex) {
//             log.error("JWT claims string is empty");
//         }
//         return false;
//     }
// }