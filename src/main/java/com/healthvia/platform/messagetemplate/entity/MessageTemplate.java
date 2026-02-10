// messagetemplate/entity/MessageTemplate.java
package com.healthvia.platform.messagetemplate.entity;

import java.util.Map;
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
@Document(collection = "message_templates")
@CompoundIndex(def = "{'category': 1, 'language': 1, 'isActive': 1}")
public class MessageTemplate extends BaseEntity {

    @NotBlank(message = "Şablon adı boş olamaz")
    @Size(max = 200)
    @Indexed
    private String name;

    @NotBlank(message = "Slug boş olamaz")
    @Indexed(unique = true)
    private String slug; // "welcome-en", "price-quote-tr"

    @NotNull(message = "Kategori belirtilmelidir")
    @Indexed
    private TemplateCategory category;

    @NotBlank(message = "Dil bilgisi boş olamaz")
    @Indexed
    private String language; // "TR", "EN", "AR", "DE"

    @NotBlank(message = "Şablon içeriği boş olamaz")
    @Size(max = 5000)
    private String content; // "Merhaba {{name}}, {{treatment}} tedavisi için..."

    @Field("content_html")
    @Size(max = 10000)
    private String contentHtml; // Email için HTML versiyon

    @Size(max = 300)
    private String description; // Agent için açıklama

    // Placeholder değişkenler: ["name", "treatment", "price", "date"]
    private Set<String> placeholders;

    // Hangi kanallarda kullanılabilir
    private Set<String> channels; // "WHATSAPP", "EMAIL", "LIVE_CHAT"

    @Field("is_active")
    @Indexed
    private Boolean isActive;

    @Field("sort_order")
    private Integer sortOrder;

    @Field("usage_count")
    private Long usageCount;

    // === ENUMS ===

    public enum TemplateCategory {
        GREETING("Karşılama"),
        PRICE_QUOTE("Fiyat Teklifi"),
        TREATMENT_INFO("Tedavi Bilgisi"),
        FOLLOW_UP("Takip"),
        APPOINTMENT("Randevu"),
        TRAVEL_INFO("Seyahat Bilgisi"),
        POST_TREATMENT("Tedavi Sonrası"),
        FEEDBACK("Geri Bildirim"),
        CLOSING("Kapanış"),
        OTHER("Diğer");

        private final String displayName;
        TemplateCategory(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }

    // === HELPER ===

    public Boolean getIsActive() {
        return isActive != null ? isActive : true;
    }

    public Long getUsageCount() {
        return usageCount != null ? usageCount : 0L;
    }

    /**
     * Placeholder'ları değerlerle değiştirerek mesaj oluştur
     */
    public String render(Map<String, String> values) {
        String rendered = this.content;
        if (values != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return rendered;
    }

    public String renderHtml(Map<String, String> values) {
        if (contentHtml == null) return render(values);
        String rendered = this.contentHtml;
        if (values != null) {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        return rendered;
    }
}
