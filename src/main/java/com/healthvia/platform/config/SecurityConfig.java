// config/SecurityConfig.java - UPDATED VERSION
package com.healthvia.platform.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.healthvia.platform.auth.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ✅ UPDATED: More permissive for testing
                .requestMatchers("/api/auth/**").permitAll()           // Authentication endpoints
                .requestMatchers("/api/test/**").permitAll()           // Test endpoints  
                .requestMatchers("/api/public/**").permitAll()         // Public endpoints
                .requestMatchers("/api/*/public/**").permitAll()       // Any public sub-paths
                .requestMatchers("/api/doctors/public/**").permitAll() // Doctor public search
                .requestMatchers("/api/patients/public/**").permitAll()// Patient public info
                .requestMatchers("/api/users/check-**").permitAll()    // Validation endpoints
                .requestMatchers("/api/patients/check-**").permitAll() // Patient validation
                .requestMatchers("/api/doctors/check-**").permitAll()  // Doctor validation
                .requestMatchers("/error").permitAll()                 // Error pages
                .requestMatchers("/").permitAll()     
                .requestMatchers("/api/patients/validate-tc-kimlik").permitAll()
                .requestMatchers("/api/patients/check-tc-kimlik").permitAll()                 // Root endpoint
                .requestMatchers("/health").permitAll()                // Health check
                .requestMatchers("/actuator/**").permitAll()           // Spring actuator
                .requestMatchers("/swagger-ui/**").permitAll()         // Swagger UI (future)
                .requestMatchers("/v3/api-docs/**").permitAll()  
                .requestMatchers("/api/v1/treatments/public/**").permitAll()      // OpenAPI docs (future)
                .requestMatchers("/api/v1/leads/public/**").permitAll()
                .requestMatchers("/api/v1/leads/webhook/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // ✅ UPDATED: More permissive for development/testing
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}