// treatment/dto/TreatmentTypeDto.java
package com.healthvia.platform.treatment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.healthvia.platform.treatment.entity.TreatmentType;
import com.healthvia.platform.treatment.entity.TreatmentType.TreatmentCategory;
import com.healthvia.platform.treatment.entity.TreatmentType.TreatmentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentTypeDto {

    private String id;

    // Temel
    private String name;
    private String nameTr;
    private String slug;
    private String summary;
    private String summaryTr;
    private String description;
    private String descriptionTr;

    // Kategori
    private TreatmentCategory category;
    private String categoryDisplayName;
    private String categoryDisplayNameTr;
    private String subCategory;
    private String medicalSpecialty;

    // Görsel
    private String iconUrl;
    private String coverImageUrl;
    private List<String> galleryUrls;

    // Fiyat
    private BigDecimal basePrice;
    private BigDecimal maxPrice;
    private String currency;
    private String priceDisplay;
    private String priceNote;
    private String priceNoteTr;

    // Süre
    private Integer procedureDurationMinutes;
    private Integer hospitalStayDays;
    private Integer recoveryDays;
    private Integer totalStayDays;
    private String stayDisplay;
    private Boolean followUpRequired;
    private String followUpNote;

    // İlişkiler
    private Set<String> availableHospitalIds;
    private Set<String> specialistDoctorIds;
    private Set<String> partnerHotelIds;

    // FAQ & paket
    private List<FaqDto> faqs;
    private List<String> inclusions;
    private List<String> inclusionsTr;
    private List<String> exclusions;
    private List<String> exclusionsTr;

    // SEO
    private String seoTitle;
    private String seoTitleTr;
    private String seoDescription;
    private String seoDescriptionTr;
    private Set<String> seoKeywords;

    // Durum
    private TreatmentStatus status;
    private Boolean isPopular;
    private Boolean isFeatured;
    private Integer sortOrder;
    private Long viewCount;
    private Long inquiryCount;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // === FACTORY METHODS ===

    public static TreatmentTypeDto fromEntity(TreatmentType t) {
        if (t == null) return null;

        return TreatmentTypeDto.builder()
                .id(t.getId())
                .name(t.getName())
                .nameTr(t.getNameTr())
                .slug(t.getSlug())
                .summary(t.getSummary())
                .summaryTr(t.getSummaryTr())
                .description(t.getDescription())
                .descriptionTr(t.getDescriptionTr())
                .category(t.getCategory())
                .categoryDisplayName(t.getCategory() != null ? t.getCategory().getDisplayName() : null)
                .categoryDisplayNameTr(t.getCategory() != null ? t.getCategory().getDisplayNameTr() : null)
                .subCategory(t.getSubCategory())
                .medicalSpecialty(t.getMedicalSpecialty())
                .iconUrl(t.getIconUrl())
                .coverImageUrl(t.getCoverImageUrl())
                .galleryUrls(t.getGalleryUrls())
                .basePrice(t.getBasePrice())
                .maxPrice(t.getMaxPrice())
                .currency(t.getCurrency())
                .priceDisplay(t.getPriceDisplay())
                .priceNote(t.getPriceNote())
                .priceNoteTr(t.getPriceNoteTr())
                .procedureDurationMinutes(t.getProcedureDurationMinutes())
                .hospitalStayDays(t.getHospitalStayDays())
                .recoveryDays(t.getRecoveryDays())
                .totalStayDays(t.getTotalStayDays())
                .stayDisplay(t.getStayDisplay())
                .followUpRequired(t.getFollowUpRequired())
                .followUpNote(t.getFollowUpNote())
                .availableHospitalIds(t.getAvailableHospitalIds())
                .specialistDoctorIds(t.getSpecialistDoctorIds())
                .partnerHotelIds(t.getPartnerHotelIds())
                .faqs(t.getFaqs() != null ? t.getFaqs().stream().map(FaqDto::fromEntity).toList() : null)
                .inclusions(t.getInclusions())
                .inclusionsTr(t.getInclusionsTr())
                .exclusions(t.getExclusions())
                .exclusionsTr(t.getExclusionsTr())
                .seoTitle(t.getSeoTitle())
                .seoTitleTr(t.getSeoTitleTr())
                .seoDescription(t.getSeoDescription())
                .seoDescriptionTr(t.getSeoDescriptionTr())
                .seoKeywords(t.getSeoKeywords())
                .status(t.getStatus())
                .isPopular(t.getIsPopular())
                .isFeatured(t.getIsFeatured())
                .sortOrder(t.getSortOrder())
                .viewCount(t.getViewCount())
                .inquiryCount(t.getInquiryCount())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    /**
     * Hafif versiyon — listeleme ve kart gösterimleri için
     */
    public static TreatmentTypeDto fromEntityBasic(TreatmentType t) {
        if (t == null) return null;

        return TreatmentTypeDto.builder()
                .id(t.getId())
                .name(t.getName())
                .nameTr(t.getNameTr())
                .slug(t.getSlug())
                .summary(t.getSummary())
                .summaryTr(t.getSummaryTr())
                .category(t.getCategory())
                .categoryDisplayName(t.getCategory() != null ? t.getCategory().getDisplayName() : null)
                .categoryDisplayNameTr(t.getCategory() != null ? t.getCategory().getDisplayNameTr() : null)
                .iconUrl(t.getIconUrl())
                .coverImageUrl(t.getCoverImageUrl())
                .basePrice(t.getBasePrice())
                .maxPrice(t.getMaxPrice())
                .currency(t.getCurrency())
                .priceDisplay(t.getPriceDisplay())
                .totalStayDays(t.getTotalStayDays())
                .stayDisplay(t.getStayDisplay())
                .isPopular(t.getIsPopular())
                .isFeatured(t.getIsFeatured())
                .build();
    }

    // === NESTED DTO ===

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaqDto {
        private String question;
        private String answer;
        private String questionTr;
        private String answerTr;
        private Integer sortOrder;

        public static FaqDto fromEntity(TreatmentType.FAQ faq) {
            if (faq == null) return null;
            return FaqDto.builder()
                    .question(faq.getQuestion())
                    .answer(faq.getAnswer())
                    .questionTr(faq.getQuestionTr())
                    .answerTr(faq.getAnswerTr())
                    .sortOrder(faq.getSortOrder())
                    .build();
        }

        public TreatmentType.FAQ toEntity() {
            return new TreatmentType.FAQ(question, answer, questionTr, answerTr, sortOrder);
        }
    }
}
