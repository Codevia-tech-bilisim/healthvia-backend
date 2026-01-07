package com.healthvia.platform.zoom.controller;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.zoom.dto.ZoomMeetingRequest;
import com.healthvia.platform.zoom.dto.ZoomMeetingResponse;
import com.healthvia.platform.zoom.service.ZoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Zoom video meeting operations
 *
 * Endpoints:
 * - POST /api/v1/zoom/meetings - Create a new meeting
 * - GET /api/v1/zoom/meetings/{id} - Get meeting details
 * - DELETE /api/v1/zoom/meetings/{id} - Delete a meeting
 * - PATCH /api/v1/zoom/meetings/{id} - Update a meeting
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/zoom")
@RequiredArgsConstructor
@Tag(name = "Zoom", description = "Video meeting operations")
public class ZoomController {

    private final ZoomService zoomService;

    /**
     * Create a new Zoom meeting
     */
    @PostMapping("/meetings")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Create Zoom meeting", description = "Create a new scheduled Zoom meeting for video consultation")
    public ResponseEntity<ApiResponse<ZoomMeetingResponse>> createMeeting(
            @Valid @RequestBody ZoomMeetingRequest request) {

        log.info("Creating Zoom meeting: topic={}, startTime={}, duration={}",
                request.getTopic(), request.getStartTime(), request.getDurationMinutes());

        ZoomMeetingResponse response = zoomService.createMeeting(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Zoom toplantısı başarıyla oluşturuldu", response));
    }

    /**
     * Get Zoom meeting details
     */
    @GetMapping("/meetings/{meetingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get meeting details", description = "Retrieve Zoom meeting details by meeting ID")
    public ResponseEntity<ApiResponse<ZoomMeetingResponse>> getMeeting(
            @Parameter(description = "Zoom meeting ID")
            @PathVariable String meetingId) {

        log.info("Fetching Zoom meeting: {}", meetingId);

        ZoomMeetingResponse response = zoomService.getMeeting(meetingId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Delete a Zoom meeting
     */
    @DeleteMapping("/meetings/{meetingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Delete meeting", description = "Delete a Zoom meeting by meeting ID")
    public ResponseEntity<ApiResponse<Void>> deleteMeeting(
            @Parameter(description = "Zoom meeting ID")
            @PathVariable String meetingId) {

        log.info("Deleting Zoom meeting: {}", meetingId);

        zoomService.deleteMeeting(meetingId);

        return ResponseEntity.ok(ApiResponse.success("Zoom toplantısı başarıyla silindi", null));
    }

    /**
     * Update a Zoom meeting
     */
    @PatchMapping("/meetings/{meetingId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR')")
    @Operation(summary = "Update meeting", description = "Update a Zoom meeting by meeting ID")
    public ResponseEntity<ApiResponse<ZoomMeetingResponse>> updateMeeting(
            @Parameter(description = "Zoom meeting ID")
            @PathVariable String meetingId,
            @Valid @RequestBody ZoomMeetingRequest request) {

        log.info("Updating Zoom meeting: {}", meetingId);

        ZoomMeetingResponse response = zoomService.updateMeeting(meetingId, request);

        return ResponseEntity.ok(ApiResponse.success("Zoom toplantısı başarıyla güncellendi", response));
    }

    /**
     * Get simplified meeting info (for frontend)
     */
    @GetMapping("/meetings/{meetingId}/info")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'PATIENT')")
    @Operation(summary = "Get simplified meeting info", description = "Get simplified meeting information for frontend display")
    public ResponseEntity<ApiResponse<ZoomMeetingResponse.SimplifiedMeetingInfo>> getMeetingInfo(
            @Parameter(description = "Zoom meeting ID")
            @PathVariable String meetingId) {

        log.info("Fetching simplified Zoom meeting info: {}", meetingId);

        ZoomMeetingResponse response = zoomService.getMeeting(meetingId);

        return ResponseEntity.ok(ApiResponse.success(response.toSimplified()));
    }
}
