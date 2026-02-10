// messagetemplate/service/impl/MessageTemplateServiceImpl.java
package com.healthvia.platform.messagetemplate.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.messagetemplate.entity.MessageTemplate;
import com.healthvia.platform.messagetemplate.entity.MessageTemplate.TemplateCategory;
import com.healthvia.platform.messagetemplate.repository.MessageTemplateRepository;
import com.healthvia.platform.messagetemplate.service.MessageTemplateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MessageTemplateServiceImpl implements MessageTemplateService {

    private final MessageTemplateRepository repository;

    @Override
    public MessageTemplate create(MessageTemplate template) {
        if (repository.existsBySlug(template.getSlug())) {
            throw new IllegalArgumentException("Bu slug zaten kullanılıyor: " + template.getSlug());
        }
        if (template.getIsActive() == null) template.setIsActive(true);
        log.info("Creating message template: {}", template.getName());
        return repository.save(template);
    }

    @Override
    public MessageTemplate update(String id, MessageTemplate updated) {
        MessageTemplate existing = findByIdOrThrow(id);

        if (updated.getName() != null) existing.setName(updated.getName());
        if (updated.getSlug() != null && !updated.getSlug().equals(existing.getSlug())) {
            if (repository.existsBySlug(updated.getSlug())) {
                throw new IllegalArgumentException("Bu slug zaten kullanılıyor: " + updated.getSlug());
            }
            existing.setSlug(updated.getSlug());
        }
        if (updated.getCategory() != null) existing.setCategory(updated.getCategory());
        if (updated.getLanguage() != null) existing.setLanguage(updated.getLanguage());
        if (updated.getContent() != null) existing.setContent(updated.getContent());
        if (updated.getContentHtml() != null) existing.setContentHtml(updated.getContentHtml());
        if (updated.getDescription() != null) existing.setDescription(updated.getDescription());
        if (updated.getPlaceholders() != null) existing.setPlaceholders(updated.getPlaceholders());
        if (updated.getChannels() != null) existing.setChannels(updated.getChannels());
        if (updated.getSortOrder() != null) existing.setSortOrder(updated.getSortOrder());

        return repository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MessageTemplate> findById(String id) {
        return repository.findById(id).filter(t -> !t.isDeleted());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MessageTemplate> findBySlug(String slug) {
        return repository.findBySlugAndDeletedFalse(slug);
    }

    @Override
    public void delete(String id, String deletedBy) {
        MessageTemplate template = findByIdOrThrow(id);
        template.markAsDeleted(deletedBy);
        repository.save(template);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageTemplate> findAll(Pageable pageable) {
        return repository.findByIsActiveTrueAndDeletedFalse(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageTemplate> findByLanguage(String language) {
        return repository.findByLanguageAndIsActiveTrueAndDeletedFalseOrderBySortOrderAsc(language);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageTemplate> findByCategoryAndLanguage(TemplateCategory category, String language) {
        return repository.findByCategoryAndLanguageAndIsActiveTrueAndDeletedFalseOrderBySortOrderAsc(
                category, language);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageTemplate> findByChannelAndLanguage(String channel, String language) {
        return repository.findByChannelAndLanguage(channel, language);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageTemplate> search(String keyword, Pageable pageable) {
        return repository.search(keyword, pageable);
    }

    @Override
    public String render(String templateId, Map<String, String> values) {
        MessageTemplate template = findByIdOrThrow(templateId);
        incrementUsageCount(templateId);
        return template.render(values);
    }

    @Override
    public String renderHtml(String templateId, Map<String, String> values) {
        MessageTemplate template = findByIdOrThrow(templateId);
        return template.renderHtml(values);
    }

    @Override
    public MessageTemplate toggleActive(String id) {
        MessageTemplate template = findByIdOrThrow(id);
        template.setIsActive(!template.getIsActive());
        return repository.save(template);
    }

    @Override
    public MessageTemplate incrementUsageCount(String id) {
        MessageTemplate template = findByIdOrThrow(id);
        template.setUsageCount(template.getUsageCount() + 1);
        return repository.save(template);
    }

    private MessageTemplate findByIdOrThrow(String id) {
        return repository.findById(id)
                .filter(t -> !t.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("MessageTemplate", "id", id));
    }
}
