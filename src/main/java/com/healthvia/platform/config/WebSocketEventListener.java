package com.healthvia.platform.config;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Online agent takibi.
 * WebSocket bağlantı/kesinti olaylarını dinler ve aktif session'ları yönetir.
 *
 * Kullanım:
 *   @Autowired WebSocketEventListener listener;
 *   boolean isOnline = listener.isAgentOnline("agentId");
 *   Set<String> onlineAgents = listener.getOnlineAgentIds();
 */
@Component
@Slf4j
public class WebSocketEventListener {

    // sessionId → userId mapping
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    // userId → Set<sessionId> (bir user birden fazla tab açabilir)
    @Getter
    private final Map<String, Set<String>> userSessionsMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        if (principal == null) return;

        String userId = principal.getName();
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");

        if (sessionId != null) {
            sessionUserMap.put(sessionId, userId);
            userSessionsMap.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

            log.info("WebSocket connected: userId={} sessionId={} (total sessions: {})",
                    userId, sessionId, userSessionsMap.get(userId).size());
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String userId = sessionUserMap.remove(sessionId);

        if (userId != null) {
            Set<String> sessions = userSessionsMap.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);
                if (sessions.isEmpty()) {
                    userSessionsMap.remove(userId);
                    log.info("WebSocket fully disconnected: userId={}", userId);
                } else {
                    log.info("WebSocket tab closed: userId={} (remaining sessions: {})",
                            userId, sessions.size());
                }
            }
        }
    }

    /**
     * Agent online mı?
     */
    public boolean isAgentOnline(String agentId) {
        Set<String> sessions = userSessionsMap.get(agentId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Online olan tüm agent ID'leri
     */
    public Set<String> getOnlineAgentIds() {
        return userSessionsMap.keySet();
    }

    /**
     * Online agent sayısı
     */
    public int getOnlineAgentCount() {
        return userSessionsMap.size();
    }

    /**
     * Toplam aktif session sayısı
     */
    public int getTotalSessionCount() {
        return sessionUserMap.size();
    }
}
