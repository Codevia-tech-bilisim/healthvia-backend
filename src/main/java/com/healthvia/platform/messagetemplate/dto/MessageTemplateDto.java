// messagetemplate/dto/MessageTemplateDto.java
package com.healthvia.platform.messagetemplate.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.healthvia.platform.messagetemplate.entity.MessageTemplate;
import com.healthvia.platform.messagetemplate.entity.MessageTemplate.TemplateCategory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageTemplateDto {

    private String id;
    private String name;
    private String slug;
    private TemplateCategory category;
    private String categoryDisplayName;
    private String language;
    private String content;
    private String contentHtml;
    private String description;
    private Set<String> placeholders;
    private Set<String> channels;
    private Boolean isActive;
    private Integer sortOrder;
    private Long usageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MessageTemplateDto fromEntity(MessageTemplate t) {
        if (t == null) return null;

        return MessageTemplateDto.builder()
                .id(t.getId())
                .name(t.getName())
                .slug(t.getSlug())
                .category(t.getCategory())
                .categoryDisplayName(t.getCategory() != null ? t.getCategory().getDisplayName() : null)
                .language(t.getLanguage())
                .content(t.getContent())
                .contentHtml(t.getContentHtml())
                .description(t.getDescription())
                .placeholders(t.getPlaceholders())
                .channels(t.getChannels())
                .isActive(t.getIsActive())
                .sortOrder(t.getSortOrder())
                .usageCount(t.getUsageCount())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }

    public static MessageTemplateDto fromEntityBasic(MessageTemplate t) {
        if (t == null) return null;

        return MessageTemplateDto.builder()
                .id(t.getId())
                .name(t.getName())
                .slug(t.getSlug())
                .category(t.getCategory())
                .categoryDisplayName(t.getCategory() != null ? t.getCategory().getDisplayName() : null)
                .language(t.getLanguage())
                .description(t.getDescription())
                .placeholders(t.getPlaceholders())
                .channels(t.getChannels())
                .isActive(t.getIsActive())
                .usageCount(t.getUsageCount())
                .build();
    }
}
