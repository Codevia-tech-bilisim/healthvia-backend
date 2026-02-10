// messagetemplate/repository/MessageTemplateRepository.java
package com.healthvia.platform.messagetemplate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthvia.platform.messagetemplate.entity.MessageTemplate;
import com.healthvia.platform.messagetemplate.entity.MessageTemplate.TemplateCategory;

@Repository
public interface MessageTemplateRepository extends MongoRepository<MessageTemplate, String> {

    Optional<MessageTemplate> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlug(String slug);

    List<MessageTemplate> findByLanguageAndIsActiveTrueAndDeletedFalseOrderBySortOrderAsc(String language);

    List<MessageTemplate> findByCategoryAndLanguageAndIsActiveTrueAndDeletedFalseOrderBySortOrderAsc(
            TemplateCategory category, String language);

    Page<MessageTemplate> findByIsActiveTrueAndDeletedFalse(Pageable pageable);

    @Query("{ 'channels': ?0, 'language': ?1, 'isActive': true, 'deleted': false }")
    List<MessageTemplate> findByChannelAndLanguage(String channel, String language);

    @Query("{ $or: [ " +
           "{'name': {$regex: ?0, $options: 'i'}}, " +
           "{'content': {$regex: ?0, $options: 'i'}}, " +
           "{'description': {$regex: ?0, $options: 'i'}} " +
           "], 'deleted': false }")
    Page<MessageTemplate> search(String keyword, Pageable pageable);

    long countByIsActiveTrueAndDeletedFalse();

    long countByCategoryAndDeletedFalse(TemplateCategory category);
}
