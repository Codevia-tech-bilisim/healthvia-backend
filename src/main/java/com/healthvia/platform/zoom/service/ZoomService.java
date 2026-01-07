package com.healthvia.platform.zoom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthvia.platform.config.ZoomProperties;
import com.healthvia.platform.zoom.dto.ZoomMeetingRequest;
import com.healthvia.platform.zoom.dto.ZoomMeetingResponse;
import com.healthvia.platform.zoom.dto.ZoomTokenResponse;
import com.healthvia.platform.zoom.exception.ZoomApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for Zoom API integration using Server-to-Server OAuth
 *
 * Features:
 * - Automatic token management with caching
 * - Meeting creation, deletion, and retrieval
 * - Thread-safe token refresh
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZoomService {

    private final ZoomProperties zoomProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Token caching
    private String cachedAccessToken;
    private Instant tokenExpiresAt;
    private final ReentrantLock tokenLock = new ReentrantLock();

    // Token refresh buffer (5 minutes before expiry)
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 300;

    /**
     * Get a valid access token, refreshing if necessary
     */
    public String getAccessToken() {
        tokenLock.lock();
        try {
            if (cachedAccessToken != null && tokenExpiresAt != null
                    && Instant.now().plusSeconds(TOKEN_REFRESH_BUFFER_SECONDS).isBefore(tokenExpiresAt)) {
                log.debug("Using cached Zoom access token");
                return cachedAccessToken;
            }

            log.info("Fetching new Zoom access token");
            ZoomTokenResponse tokenResponse = fetchAccessToken();

            cachedAccessToken = tokenResponse.getAccessToken();
            tokenExpiresAt = Instant.now().plusSeconds(tokenResponse.getExpiresIn());

            log.info("Zoom access token obtained, expires at: {}", tokenExpiresAt);
            return cachedAccessToken;

        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Fetch access token from Zoom OAuth endpoint using Server-to-Server OAuth
     */
    private ZoomTokenResponse fetchAccessToken() {
        String credentials = zoomProperties.getClientId() + ":" + zoomProperties.getClientSecret();
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encodedCredentials);

        String body = "grant_type=account_credentials&account_id=" + zoomProperties.getAccountId();

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<ZoomTokenResponse> response = restTemplate.exchange(
                    zoomProperties.getOauthUrl(),
                    HttpMethod.POST,
                    request,
                    ZoomTokenResponse.class
            );

            if (response.getBody() == null || response.getBody().getAccessToken() == null) {
                throw new ZoomApiException("Zoom token yanıtı geçersiz", HttpStatus.BAD_GATEWAY);
            }

            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("Zoom OAuth error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ZoomApiException("Zoom kimlik doğrulama hatası: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY, e);
        } catch (Exception e) {
            log.error("Zoom OAuth request failed", e);
            throw new ZoomApiException("Zoom kimlik doğrulama başarısız: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new Zoom meeting
     */
    public ZoomMeetingResponse createMeeting(ZoomMeetingRequest request) {
        log.info("Creating Zoom meeting: {}", request.getTopic());

        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        // Build Zoom API request body
        Map<String, Object> meetingData = buildMeetingRequestBody(request);

        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(meetingData, headers);

        try {
            String url = zoomProperties.getBaseUrl() + "/users/me/meetings";

            ResponseEntity<ZoomMeetingResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpRequest,
                    ZoomMeetingResponse.class
            );

            if (response.getBody() == null) {
                throw new ZoomApiException("Zoom meeting oluşturma yanıtı boş", HttpStatus.BAD_GATEWAY);
            }

            log.info("Zoom meeting created successfully. ID: {}", response.getBody().getId());
            return response.getBody();

        } catch (HttpClientErrorException e) {
            log.error("Zoom meeting creation error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            handleZoomError(e);
            throw new ZoomApiException("Zoom meeting oluşturma hatası: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY, e);
        } catch (ZoomApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Zoom meeting creation failed", e);
            throw new ZoomApiException("Zoom meeting oluşturulamadı: " + e.getMessage(), e);
        }
    }

    /**
     * Get meeting details by ID
     */
    public ZoomMeetingResponse getMeeting(String meetingId) {
        log.info("Fetching Zoom meeting: {}", meetingId);

        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            String url = zoomProperties.getBaseUrl() + "/meetings/" + meetingId;

            ResponseEntity<ZoomMeetingResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    request,
                    ZoomMeetingResponse.class
            );

            return response.getBody();

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Zoom meeting not found: {}", meetingId);
                throw new ZoomApiException("Zoom meeting bulunamadı: " + meetingId,
                        HttpStatus.NOT_FOUND, "MEETING_NOT_FOUND");
            }
            log.error("Zoom meeting fetch error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ZoomApiException("Zoom meeting bilgisi alınamadı: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY, e);
        } catch (ZoomApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Zoom meeting fetch failed", e);
            throw new ZoomApiException("Zoom meeting bilgisi alınamadı: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a Zoom meeting by ID
     */
    public void deleteMeeting(String meetingId) {
        log.info("Deleting Zoom meeting: {}", meetingId);

        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            String url = zoomProperties.getBaseUrl() + "/meetings/" + meetingId;

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    request,
                    Void.class
            );

            log.info("Zoom meeting deleted successfully: {}", meetingId);

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Zoom meeting already deleted or not found: {}", meetingId);
                // Not found is acceptable for delete operations
                return;
            }
            log.error("Zoom meeting deletion error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ZoomApiException("Zoom meeting silinemedi: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY, e);
        } catch (Exception e) {
            log.error("Zoom meeting deletion failed", e);
            throw new ZoomApiException("Zoom meeting silinemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Update a Zoom meeting
     */
    public ZoomMeetingResponse updateMeeting(String meetingId, ZoomMeetingRequest request) {
        log.info("Updating Zoom meeting: {}", meetingId);

        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);

        Map<String, Object> meetingData = buildMeetingRequestBody(request);

        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(meetingData, headers);

        try {
            String url = zoomProperties.getBaseUrl() + "/meetings/" + meetingId;

            restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    httpRequest,
                    Void.class
            );

            // Fetch updated meeting details
            return getMeeting(meetingId);

        } catch (HttpClientErrorException e) {
            log.error("Zoom meeting update error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ZoomApiException("Zoom meeting güncellenemedi: " + e.getMessage(),
                    HttpStatus.BAD_GATEWAY, e);
        } catch (ZoomApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Zoom meeting update failed", e);
            throw new ZoomApiException("Zoom meeting güncellenemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Build Zoom API meeting request body
     */
    private Map<String, Object> buildMeetingRequestBody(ZoomMeetingRequest request) {
        Map<String, Object> meetingData = new HashMap<>();

        meetingData.put("topic", request.getTopic());
        meetingData.put("type", 2); // Scheduled meeting
        meetingData.put("duration", request.getDurationMinutes());
        meetingData.put("timezone", request.getTimezone());

        // Format start time for Zoom API (ISO 8601)
        if (request.getStartTime() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                    .withZone(ZoneId.of(request.getTimezone()));
            String startTimeStr = request.getStartTime()
                    .atZone(ZoneId.of(request.getTimezone()))
                    .format(formatter);
            meetingData.put("start_time", startTimeStr);
        }

        if (request.getAgenda() != null) {
            meetingData.put("agenda", request.getAgenda());
        }

        // Meeting settings
        Map<String, Object> settings = new HashMap<>();
        settings.put("host_video", true);
        settings.put("participant_video", true);
        settings.put("join_before_host", request.getJoinBeforeHost());
        settings.put("mute_upon_entry", request.getMuteOnEntry());
        settings.put("waiting_room", request.getWaitingRoom());
        settings.put("approval_type", 2); // No registration required
        settings.put("audio", "both"); // Both telephony and VoIP
        settings.put("auto_recording", "none");

        meetingData.put("settings", settings);

        return meetingData;
    }

    /**
     * Handle Zoom API errors with appropriate exception mapping
     */
    private void handleZoomError(HttpClientErrorException e) {
        HttpStatus status = (HttpStatus) e.getStatusCode();

        switch (status) {
            case UNAUTHORIZED:
                // Clear cached token on auth errors
                tokenLock.lock();
                try {
                    cachedAccessToken = null;
                    tokenExpiresAt = null;
                } finally {
                    tokenLock.unlock();
                }
                throw new ZoomApiException("Zoom kimlik doğrulama hatası. Lütfen credentials kontrol edin.",
                        HttpStatus.BAD_GATEWAY, "ZOOM_AUTH_ERROR");

            case FORBIDDEN:
                throw new ZoomApiException("Zoom API erişim yetkisi yok. Scopes kontrol edin.",
                        HttpStatus.FORBIDDEN, "ZOOM_FORBIDDEN");

            case TOO_MANY_REQUESTS:
                throw new ZoomApiException("Zoom API rate limit aşıldı. Lütfen bekleyin.",
                        HttpStatus.TOO_MANY_REQUESTS, "ZOOM_RATE_LIMITED");

            default:
                // Let other errors be handled by the caller
                break;
        }
    }

    /**
     * Invalidate the cached token (useful for testing or force refresh)
     */
    public void invalidateToken() {
        tokenLock.lock();
        try {
            cachedAccessToken = null;
            tokenExpiresAt = null;
            log.info("Zoom access token invalidated");
        } finally {
            tokenLock.unlock();
        }
    }
}
