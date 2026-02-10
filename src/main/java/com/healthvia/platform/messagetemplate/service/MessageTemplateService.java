// messagetemplate/service/MessageTemplateService.java
package com.healthvia.platform.messagetemplate.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.healthvia.platform.messagetemplate.entity.MessageTemplate;
import com.healthvia.platform.messagetemplate.entity.MessageTemplate.TemplateCategory;

public interface MessageTemplateService {

    // === CRUD ===
    MessageTemplate create(MessageTemplate template);
    MessageTemplate update(String id, MessageTemplate template);
    Optional<MessageTemplate> findById(String id);
    Optional<MessageTemplate> findBySlug(String slug);
    void delete(String id, String deletedBy);

    // === SORGULAR ===
    Page<MessageTemplate> findAll(Pageable pageable);
    List<MessageTemplate> findByLanguage(String language);
    List<MessageTemplate> findByCategoryAndLanguage(TemplateCategory category, String language);
    List<MessageTemplate> findByChannelAndLanguage(String channel, String language);
    Page<MessageTemplate> search(String keyword, Pageable pageable);

    // === RENDER ===
    String render(String templateId, Map<String, String> values);
    String renderHtml(String templateId, Map<String, String> values);

    // === YÖNETİM ===
    MessageTemplate toggleActive(String id);
    MessageTemplate incrementUsageCount(String id);
}
