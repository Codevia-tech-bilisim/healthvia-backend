package com.healthvia.platform.zoom.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for Zoom meeting operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ZoomMeetingResponse {

    /**
     * Zoom meeting ID
     */
    private Long id;

    /**
     * Meeting UUID
     */
    private String uuid;

    /**
     * Host ID
     */
    @JsonProperty("host_id")
    private String hostId;

    /**
     * Host email
     */
    @JsonProperty("host_email")
    private String hostEmail;

    /**
     * Meeting topic
     */
    private String topic;

    /**
     * Meeting type (1=instant, 2=scheduled, 3=recurring no fixed time, 8=recurring fixed time)
     */
    private Integer type;

    /**
     * Meeting status
     */
    private String status;

    /**
     * Start time in ISO 8601 format
     */
    @JsonProperty("start_time")
    private String startTime;

    /**
     * Meeting duration in minutes
     */
    private Integer duration;

    /**
     * Timezone
     */
    private String timezone;

    /**
     * Meeting agenda
     */
    private String agenda;

    /**
     * URL for participants to join the meeting
     */
    @JsonProperty("join_url")
    private String joinUrl;

    /**
     * URL for host to start the meeting
     */
    @JsonProperty("start_url")
    private String startUrl;

    /**
     * Meeting password
     */
    private String password;

    /**
     * Encrypted meeting password for join URL
     */
    @JsonProperty("encrypted_password")
    private String encryptedPassword;

    /**
     * Personal Meeting ID
     */
    private Long pmi;

    /**
     * Created at timestamp
     */
    @JsonProperty("created_at")
    private String createdAt;

    /**
     * Meeting settings
     */
    private MeetingSettings settings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MeetingSettings {

        @JsonProperty("host_video")
        private Boolean hostVideo;

        @JsonProperty("participant_video")
        private Boolean participantVideo;

        @JsonProperty("join_before_host")
        private Boolean joinBeforeHost;

        @JsonProperty("mute_upon_entry")
        private Boolean muteUponEntry;

        @JsonProperty("waiting_room")
        private Boolean waitingRoom;

        @JsonProperty("use_pmi")
        private Boolean usePmi;

        @JsonProperty("approval_type")
        private Integer approvalType;

        @JsonProperty("audio")
        private String audio;

        @JsonProperty("auto_recording")
        private String autoRecording;
    }

    /**
     * Convert to simplified DTO for frontend
     */
    public SimplifiedMeetingInfo toSimplified() {
        return SimplifiedMeetingInfo.builder()
                .meetingId(String.valueOf(id))
                .joinUrl(joinUrl)
                .startUrl(startUrl)
                .password(password)
                .topic(topic)
                .startTime(startTime)
                .duration(duration)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimplifiedMeetingInfo {
        private String meetingId;
        private String joinUrl;
        private String startUrl;
        private String password;
        private String topic;
        private String startTime;
        private Integer duration;
    }
}
