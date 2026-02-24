package com.healthvia.platform.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.healthvia.platform.auth.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket STOMP bağlantılarında JWT doğrulaması.
 *
 * Client bağlantı örneği (JS):
 * <pre>
 * const socket = new SockJS('/ws');
 * const stompClient = Stomp.over(socket);
 * stompClient.connect(
 *   { Authorization: 'Bearer eyJhbG...' },
 *   onConnected,
 *   onError
 * );
 * </pre>
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);

                if (accessor == null) return message;

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);

                        try {
                            if (jwtTokenProvider.validateToken(token)) {
                                String userId = jwtTokenProvider.getUserIdFromToken(token);
                                String role = jwtTokenProvider.getRoleFromToken(token).name();

                                UsernamePasswordAuthenticationToken auth =
                                        new UsernamePasswordAuthenticationToken(
                                                userId, null,
                                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                                        );

                                SecurityContextHolder.getContext().setAuthentication(auth);
                                accessor.setUser(auth);

                                log.info("WebSocket connected: userId={} role={}", userId, role);
                            } else {
                                log.warn("WebSocket connection rejected: invalid JWT");
                                throw new IllegalArgumentException("Geçersiz token");
                            }
                        } catch (Exception e) {
                            log.warn("WebSocket JWT auth failed: {}", e.getMessage());
                            throw new IllegalArgumentException("Token doğrulama hatası");
                        }
                    } else {
                        log.warn("WebSocket connection without Authorization header");
                        // Public WebSocket'e izin vermek istersen burayı kaldır
                        throw new IllegalArgumentException("Authorization header gerekli");
                    }
                }

                return message;
            }
        });
    }
}
