// lead/entity/Lead.java
package com.healthvia.platform.lead.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.healthvia.platform.common.model.BaseEntity;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document(collection = "leads")
@CompoundIndex(def = "{'status': 1, 'assignedAgentId': 1}")
@CompoundIndex(def = "{'source': 1, 'createdAt': -1}")
@CompoundIndex(def = "{'language': 1, 'status': 1}")
public class Lead extends BaseEntity {

    // === İLETİŞİM BİLGİLERİ ===

    @Field("first_name")
    private String firstName;

    @Field("last_name")
    private String lastName;

    @Email(message = "Geçerli bir email adresi giriniz")
    @Indexed
    private String email;

    @Indexed
    private String phone;

    @Field("whatsapp_number")
    private String whatsappNumber;

    @Field("instagram_handle")
    private String instagramHandle;

    // === KAYNAK & KANAL ===

    @NotNull(message = "Lead kaynağı belirtilmelidir")
    @Indexed
    private LeadSource source;

    @Field("source_detail")
    private String sourceDetail; // Ör: "Instagram DM - hair transplant post", "Google Ads - dental campaign"

    @Field("utm_source")
    private String utmSource;

    @Field("utm_medium")
    private String utmMedium;

    @Field("utm_campaign")
    private String utmCampaign;

    @Field("landing_page")
    private String landingPage; // İlk giriş yaptığı sayfa

    // === DİL & KONUM ===

    @NotBlank(message = "Dil bilgisi boş olamaz")
    @Indexed
    private String language; // "TR", "EN", "AR", "DE", "RU", "FR"

    private String country;

    private String city;

    private String timezone;

    @Field("ip_address")
    private String ipAddress;

    // === İLGİ ALANI ===

    @Field("interested_treatment")
    private String interestedTreatment; // TreatmentCategory enum değeri veya serbest metin

    @Field("treatment_type_id")
    private String treatmentTypeId; // TreatmentType referansı

    @Field("interested_treatments")
    private Set<String> interestedTreatments; // Birden fazla ilgi varsa

    @Field("budget_range")
    private String budgetRange; // "1000-3000 USD", "5000+ EUR"

    @Field("preferred_dates")
    private String preferredDates; // "March 2026", "ASAP"

    @Field("initial_message")
    @Size(max = 2000)
    private String initialMessage; // İlk gelen mesaj

    @Size(max = 2000)
    private String notes; // Agent notları

    // === DURUM YÖNETİMİ ===

    @NotNull(message = "Lead durumu belirtilmelidir")
    @Indexed
    private LeadStatus status;

    @Field("previous_status")
    private LeadStatus previousStatus;

    @Field("status_changed_at")
    private LocalDateTime statusChangedAt;

    @Field("status_changed_by")
    private String statusChangedBy;

    @Field("status_change_reason")
    private String statusChangeReason;

    // === ÖNCELİK ===

    @Indexed
    private LeadPriority priority;

    // === ATAMA ===

    @Field("assigned_agent_id")
    @Indexed
    private String assignedAgentId;

    @Field("assigned_agent_name")
    private String assignedAgentName;

    @Field("assigned_at")
    private LocalDateTime assignedAt;

    @Field("assignment_method")
    private AssignmentMethod assignmentMethod; // AUTO veya MANUAL

    @Field("previous_agent_ids")
    private List<String> previousAgentIds; // Transfer geçmişi

    // === ETİKETLER ===

    @Indexed
    private Set<String> tags; // "VIP", "URGENT", "RETURNING", "HOT"

    // === TEKRAR EDEN HASTA ===

    @Field("is_returning")
    private Boolean isReturning;

    @Field("existing_patient_id")
    private String existingPatientId; // Eşleşen Patient kaydı

    @Field("previous_treatments")
    private List<String> previousTreatments; // Önceki tedaviler

    // === DÖNÜŞÜM TAKİBİ ===

    @Field("conversation_id")
    private String conversationId; // İlişkili konuşma

    @Field("converted_patient_id")
    private String convertedPatientId; // Dönüşüm yapıldıysa

    @Field("converted_at")
    private LocalDateTime convertedAt;

    @Field("conversion_value")
    private java.math.BigDecimal conversionValue; // Dönüşüm tutarı

    @Field("lost_reason")
    private String lostReason; // Kaybedilme nedeni

    // === ZAMANLAMA ===

    @Field("first_response_at")
    private LocalDateTime firstResponseAt;

    @Field("first_response_time_seconds")
    private Long firstResponseTimeSeconds;

    @Field("last_contact_at")
    private LocalDateTime lastContactAt;

    @Field("next_follow_up_at")
    private LocalDateTime nextFollowUpAt;

    @Field("follow_up_count")
    private Integer followUpCount;

    // === ENUMS ===

    public enum LeadSource {
        WHATSAPP("WhatsApp"),
        INSTAGRAM("Instagram DM"),
        TELEGRAM("Telegram"),
        EMAIL("Email"),
        WEB_FORM("Web Form"),
        PHONE("Telefon"),
        LIVE_CHAT("Canlı Chat"),
        REFERRAL("Referans"),
        GOOGLE_ADS("Google Ads"),
        FACEBOOK_ADS("Facebook Ads"),
        PARTNER("Partner"),
        WALK_IN("Yüz Yüze"),
        OTHER("Diğer");

        private final String displayName;
        LeadSource(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum LeadStatus {
        NEW("Yeni"),                      // İlk gelen, henüz atanmamış
        ASSIGNED("Atandı"),               // Agente atandı, henüz yanıt yok
        CONTACTED("İletişime Geçildi"),   // İlk yanıt verildi
        QUALIFIED("Nitelikli"),           // Tedavi ihtiyacı doğrulandı
        PROPOSAL_SENT("Teklif Gönderildi"), // Fiyat/paket teklifi gönderildi
        NEGOTIATION("Pazarlık"),          // Fiyat görüşmesi devam
        APPOINTMENT_SCHEDULED("Randevu Planlandı"), // Randevu alındı
        CONVERTED("Dönüştürüldü"),        // Hasta kaydı oluşturuldu
        LOST("Kaybedildi"),               // İlgi kaybı veya ret
        FOLLOW_UP("Takipte"),             // İleride tekrar ulaşılacak
        SPAM("Spam"),                     // Geçersiz/spam
        ARCHIVED("Arşiv");               // Eski kayıtlar

        private final String displayName;
        LeadStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum LeadPriority {
        LOW("Düşük"),
        MEDIUM("Orta"),
        HIGH("Yüksek"),
        URGENT("Acil");

        private final String displayName;
        LeadPriority(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    public enum AssignmentMethod {
        AUTO("Otomatik"),
        MANUAL("Manuel"),
        ROUND_ROBIN("Sıralı"),
        LANGUAGE_BASED("Dil Bazlı"),
        SPECIALIZATION_BASED("Uzmanlık Bazlı");

        private final String displayName;
        AssignmentMethod(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // === HELPER METHODS ===

    public boolean isNew() {
        return LeadStatus.NEW.equals(status);
    }

    public boolean isActive() {
        return status != null && !Set.of(
                LeadStatus.CONVERTED, LeadStatus.LOST,
                LeadStatus.SPAM, LeadStatus.ARCHIVED
        ).contains(status);
    }

    public boolean isConverted() {
        return LeadStatus.CONVERTED.equals(status);
    }

    public boolean needsFollowUp() {
        return nextFollowUpAt != null && nextFollowUpAt.isBefore(LocalDateTime.now());
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName);
        if (lastName != null) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(lastName);
        }
        return sb.isEmpty() ? "İsimsiz Lead" : sb.toString();
    }

    public String getContactDisplay() {
        if (phone != null) return phone;
        if (whatsappNumber != null) return whatsappNumber;
        if (email != null) return email;
        if (instagramHandle != null) return "@" + instagramHandle;
        return "İletişim bilgisi yok";
    }

    public Boolean getIsReturning() {
        return isReturning != null ? isReturning : false;
    }

    public Integer getFollowUpCount() {
        return followUpCount != null ? followUpCount : 0;
    }

    public void changeStatus(LeadStatus newStatus, String changedBy, String reason) {
        this.previousStatus = this.status;
        this.status = newStatus;
        this.statusChangedAt = LocalDateTime.now();
        this.statusChangedBy = changedBy;
        this.statusChangeReason = reason;
    }
}
