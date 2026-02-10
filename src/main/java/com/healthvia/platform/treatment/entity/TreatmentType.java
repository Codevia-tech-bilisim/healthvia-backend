// treatment/entity/TreatmentType.java
package com.healthvia.platform.treatment.entity;

import java.math.BigDecimal;
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
@Document(collection = "treatment_types")
@CompoundIndex(def = "{'category': 1, 'status': 1}")
@CompoundIndex(def = "{'isPopular': 1, 'sortOrder': 1}")
public class TreatmentType extends BaseEntity {

    // === TEMEL BİLGİLER ===

    @NotBlank(message = "Tedavi adı boş olamaz")
    @Size(max = 200, message = "Tedavi adı en fazla 200 karakter olabilir")
    @Indexed
    private String name; // "Hair Transplant"

    @NotBlank(message = "Tedavi adı (TR) boş olamaz")
    @Size(max = 200)
    @Field("name_tr")
    private String nameTr; // "Saç Ekimi"

    @NotBlank(message = "Slug boş olamaz")
    @Indexed(unique = true)
    private String slug; // "hair-transplant"

    @Size(max = 500, message = "Kısa açıklama en fazla 500 karakter olabilir")
    private String summary; // Kısa açıklama (EN)

    @Size(max = 500)
    @Field("summary_tr")
    private String summaryTr; // Kısa açıklama (TR)

    @Size(max = 5000, message = "Detaylı açıklama en fazla 5000 karakter olabilir")
    private String description; // Detaylı açıklama (EN)

    @Size(max = 5000)
    @Field("description_tr")
    private String descriptionTr; // Detaylı açıklama (TR)

    // === KATEGORİ & SINIFLANDIRMA ===

    @NotNull(message = "Kategori belirtilmelidir")
    @Indexed
    private TreatmentCategory category;

    @Field("sub_category")
    private String subCategory; // Ör: "FUE", "DHI" (saç ekimi alt tipi)

    @Field("medical_specialty")
    private String medicalSpecialty; // İlgili tıbbi uzmanlık: "Plastic Surgery", "Ophthalmology"

    // === GÖRSEL ===

    @Field("icon_url")
    private String iconUrl; // Kategori ikonu

    @Field("cover_image_url")
    private String coverImageUrl; // Kapak görseli

    @Field("gallery_urls")
    private List<String> galleryUrls; // Galeri görselleri (before/after vs.)

    // === FİYATLANDIRMA ===

    @Field("base_price")
    @DecimalMin(value = "0.0", message = "Fiyat negatif olamaz")
    private BigDecimal basePrice; // Başlangıç fiyatı (USD)

    @Field("max_price")
    private BigDecimal maxPrice; // Üst fiyat aralığı

    @Field("currency")
    private String currency; // "USD", "EUR", "TRY"

    @Field("price_note")
    private String priceNote; // "Fiyat greft sayısına göre değişir"

    @Field("price_note_tr")
    private String priceNoteTr;

    // === SÜRE & LOJİSTİK ===

    @Field("procedure_duration_minutes")
    @Min(value = 15, message = "İşlem süresi en az 15 dakika olmalı")
    private Integer procedureDurationMinutes; // İşlem süresi

    @Field("hospital_stay_days")
    private Integer hospitalStayDays; // Hastanede kalış süresi

    @Field("recovery_days")
    private Integer recoveryDays; // İyileşme süresi (gün)

    @Field("total_stay_days")
    private Integer totalStayDays; // Toplam Türkiye'de kalış süresi

    @Field("follow_up_required")
    private Boolean followUpRequired; // Kontrol randevusu gerekli mi

    @Field("follow_up_note")
    private String followUpNote; // "6 ay sonra kontrol"

    // === İLİŞKİLER ===

    @Field("available_hospital_ids")
    private Set<String> availableHospitalIds; // Bu tedaviyi yapan hastaneler

    @Field("specialist_doctor_ids")
    private Set<String> specialistDoctorIds; // Bu tedavide uzman doktorlar

    @Field("partner_hotel_ids")
    private Set<String> partnerHotelIds; // Önerilen oteller

    // === SSS (FAQ) ===

    @Field("faqs")
    private List<FAQ> faqs;

    // === PAKETİN İÇERDİKLERİ ===

    @Field("inclusions")
    private List<String> inclusions; // Dahil olanlar: "Airport transfer", "Hotel", "Interpreter"

    @Field("inclusions_tr")
    private List<String> inclusionsTr;

    @Field("exclusions")
    private List<String> exclusions; // Dahil olmayanlar: "Flight tickets", "Travel insurance"

    @Field("exclusions_tr")
    private List<String> exclusionsTr;

    // === SEO & GÖRÜNÜRLÜK ===

    @Field("seo_title")
    private String seoTitle;

    @Field("seo_title_tr")
    private String seoTitleTr;

    @Field("seo_description")
    @Size(max = 300)
    private String seoDescription;

    @Field("seo_description_tr")
    @Size(max = 300)
    private String seoDescriptionTr;

    @Field("seo_keywords")
    private Set<String> seoKeywords;

    // === DURUM & SIRALAMA ===

    @Indexed
    private TreatmentStatus status;

    @Field("is_popular")
    private Boolean isPopular;

    @Field("is_featured")
    private Boolean isFeatured; // Ana sayfada gösterilsin mi

    @Field("sort_order")
    private Integer sortOrder; // Sıralama önceliği

    @Field("view_count")
    private Long viewCount; // Görüntülenme sayısı

    @Field("inquiry_count")
    private Long inquiryCount; // Bu tedavi için gelen lead sayısı

    // === NESTED CLASSES ===

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FAQ {
        private String question;
        private String answer;

        @Field("question_tr")
        private String questionTr;

        @Field("answer_tr")
        private String answerTr;

        @Field("sort_order")
        private Integer sortOrder;
    }

    // === ENUMS ===

    public enum TreatmentCategory {
        // Estetik & Plastik Cerrahi
        HAIR_TRANSPLANT("Hair Transplant", "Saç Ekimi"),
        RHINOPLASTY("Rhinoplasty", "Burun Estetiği"),
        FACELIFT("Facelift", "Yüz Germe"),
        BREAST_SURGERY("Breast Surgery", "Meme Estetiği"),
        LIPOSUCTION("Liposuction", "Liposuction"),
        TUMMY_TUCK("Tummy Tuck", "Karın Germe"),
        BBL("Brazilian Butt Lift", "BBL"),

        // Diş
        DENTAL_IMPLANT("Dental Implant", "Diş İmplant"),
        DENTAL_VENEER("Dental Veneer", "Diş Kaplama"),
        DENTAL_CROWN("Dental Crown", "Diş Kron"),
        HOLLYWOOD_SMILE("Hollywood Smile", "Hollywood Gülüşü"),
        TEETH_WHITENING("Teeth Whitening", "Diş Beyazlatma"),

        // Göz
        LASIK("LASIK Eye Surgery", "Göz Lazeri"),
        CATARACT("Cataract Surgery", "Katarakt Ameliyatı"),
        LENS_REPLACEMENT("Lens Replacement", "Göz İçi Lens"),

        // Ortopedi
        KNEE_REPLACEMENT("Knee Replacement", "Diz Protezi"),
        HIP_REPLACEMENT("Hip Replacement", "Kalça Protezi"),
        SPINE_SURGERY("Spine Surgery", "Omurga Cerrahisi"),
        SPORTS_MEDICINE("Sports Medicine", "Spor Hekimliği"),

        // Kardiyoloji
        HEART_BYPASS("Heart Bypass", "Kalp Bypass"),
        ANGIOPLASTY("Angioplasty", "Anjiyoplasti"),
        HEART_VALVE("Heart Valve Surgery", "Kalp Kapak Ameliyatı"),
        CARDIAC_CHECKUP("Cardiac Check-Up", "Kardiyolojik Check-Up"),

        // Bariatrik Cerrahi
        GASTRIC_SLEEVE("Gastric Sleeve", "Tüp Mide"),
        GASTRIC_BYPASS("Gastric Bypass", "Mide Bypass"),
        GASTRIC_BALLOON("Gastric Balloon", "Mide Balonu"),

        // Onkoloji
        CANCER_SCREENING("Cancer Screening", "Kanser Taraması"),
        RADIATION_THERAPY("Radiation Therapy", "Radyoterapi"),
        CHEMOTHERAPY("Chemotherapy", "Kemoterapi"),

        // Nöroloji
        BRAIN_SURGERY("Brain Surgery", "Beyin Cerrahisi"),
        NEUROLOGY_CHECKUP("Neurology Check-Up", "Nöroloji Muayenesi"),

        // Üroloji
        KIDNEY_TRANSPLANT("Kidney Transplant", "Böbrek Nakli"),
        PROSTATE_SURGERY("Prostate Surgery", "Prostat Ameliyatı"),

        // Dermatoloji
        SKIN_TREATMENT("Skin Treatment", "Cilt Tedavisi"),
        BOTOX("Botox", "Botoks"),
        DERMAL_FILLER("Dermal Filler", "Dolgu"),
        PRP_THERAPY("PRP Therapy", "PRP Tedavisi"),

        // Göğüs Hastalıkları
        PULMONARY_CHECKUP("Pulmonary Check-Up", "Göğüs Hastalıkları Kontrolü"),

        // Genel Check-Up
        FULL_BODY_CHECKUP("Full Body Check-Up", "Genel Check-Up"),
        EXECUTIVE_CHECKUP("Executive Check-Up", "Yönetici Check-Up"),

        // Fertilite (Tüp Bebek)
        IVF("IVF Treatment", "Tüp Bebek"),
        EGG_FREEZING("Egg Freezing", "Yumurta Dondurma"),

        // Diğer
        OTHER("Other", "Diğer");

        private final String displayName;
        private final String displayNameTr;

        TreatmentCategory(String displayName, String displayNameTr) {
            this.displayName = displayName;
            this.displayNameTr = displayNameTr;
        }

        public String getDisplayName() { return displayName; }
        public String getDisplayNameTr() { return displayNameTr; }
    }

    public enum TreatmentStatus {
        ACTIVE("Aktif"),
        INACTIVE("Pasif"),
        COMING_SOON("Yakında"),
        DISCONTINUED("Sonlandırıldı");

        private final String displayName;

        TreatmentStatus(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // === HELPER METHODS ===

    public boolean isActive() {
        return TreatmentStatus.ACTIVE.equals(status) && !isDeleted();
    }

    public String getPriceDisplay() {
        if (basePrice == null) return "Fiyat için iletişime geçin";
        if (maxPrice != null) {
            return String.format("%s %s - %s %s", currency, basePrice, currency, maxPrice);
        }
        return String.format("%s %s'den başlayan fiyatlarla", currency, basePrice);
    }

    public String getStayDisplay() {
        if (totalStayDays == null) return "Belirtilmemiş";
        int nights = totalStayDays - 1;
        return String.format("%d Nights / %d Days", nights, totalStayDays);
    }

    public Boolean getIsPopular() {
        return isPopular != null ? isPopular : false;
    }

    public Boolean getIsFeatured() {
        return isFeatured != null ? isFeatured : false;
    }

    public Long getViewCount() {
        return viewCount != null ? viewCount : 0L;
    }

    public Long getInquiryCount() {
        return inquiryCount != null ? inquiryCount : 0L;
    }
}
