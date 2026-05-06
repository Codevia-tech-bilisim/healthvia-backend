// conversation/service/impl/ConversationServiceImpl.java
package com.healthvia.platform.conversation.service.impl;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.healthvia.platform.common.exception.ResourceNotFoundException;
import com.healthvia.platform.conversation.entity.Conversation;
import com.healthvia.platform.conversation.entity.Conversation.*;
import com.healthvia.platform.conversation.repository.ConversationRepository;
import com.healthvia.platform.conversation.service.ConversationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository repository;
    private final MongoTemplate mongoTemplate;

    // === CRUD ===

    @Override
    public Conversation create(Conversation conversation) {
        if (conversation.getStatus() == null) {
            conversation.setStatus(ConversationStatus.OPEN);
        }
        if (conversation.getPriority() == null) {
            conversation.setPriority(ConversationPriority.NORMAL);
        }
        conversation.setStatusChangedAt(LocalDateTime.now());

        log.info("Creating conversation for lead: {} on channel: {}", 
                conversation.getLeadId(), conversation.getChannel());
        return repository.save(conversation);
    }

    @Override
    public Conversation update(String id, Conversation updated) {
        Conversation existing = findByIdOrThrow(id);

        if (updated.getSubject() != null) existing.setSubject(updated.getSubject());
        if (updated.getLanguage() != null) existing.setLanguage(updated.getLanguage());
        if (updated.getTreatmentInterest() != null) existing.setTreatmentInterest(updated.getTreatmentInterest());
        if (updated.getPriority() != null) existing.setPriority(updated.getPriority());

        return repository.save(existing);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Conversation> findById(String id) {
        return repository.findById(id).filter(c -> !c.isDeleted());
    }

    @Override
    public void delete(String id, String deletedBy) {
        Conversation conv = findByIdOrThrow(id);
        conv.markAsDeleted(deletedBy);
        repository.save(conv);
    }

    // === KONUŞMA BAŞLATMA ===

    @Override
    public Conversation getOrCreateForLead(String leadId, Channel channel, String agentId) {
        // Aynı lead + kanal için açık konuşma var mı?
        Optional<Conversation> existing = repository
                .findByLeadIdAndChannelAndStatusNotAndDeletedFalse(
                        leadId, channel, ConversationStatus.ARCHIVED);

        if (existing.isPresent()) {
            Conversation conv = existing.get();
            // Agent değiştiyse güncelle
            if (agentId != null && !agentId.equals(conv.getAssignedAgentId())) {
                conv.setAssignedAgentId(agentId);
            }
            return repository.save(conv);
        }

        // Yeni konuşma oluştur
        Conversation newConv = Conversation.builder()
                .leadId(leadId)
                .channel(channel)
                .assignedAgentId(agentId)
                .status(ConversationStatus.OPEN)
                .priority(ConversationPriority.NORMAL)
                .statusChangedAt(LocalDateTime.now())
                .build();

        log.info("New conversation created for lead: {} channel: {}", leadId, channel);
        return repository.save(newConv);
    }

    // === SORGULAR ===

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> findByLeadId(String leadId) {
        return repository.findByLeadIdAndDeletedFalseOrderByLastMessageAtDesc(leadId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Conversation> findByAgent(String agentId, Pageable pageable) {
        return repository.findByAssignedAgentIdAndDeletedFalseOrderByLastMessageAtDesc(agentId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Conversation> findByAgentAndStatus(String agentId, ConversationStatus status, Pageable pageable) {
        return repository.findByAssignedAgentIdAndStatusAndDeletedFalseOrderByLastMessageAtDesc(
                agentId, status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Conversation> findByStatus(ConversationStatus status, Pageable pageable) {
        return repository.findByStatusAndDeletedFalseOrderByLastMessageAtDesc(status, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Conversation> findByChannel(Channel channel, Pageable pageable) {
        return repository.findByChannelAndDeletedFalse(channel, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Conversation> search(String keyword, Pageable pageable) {
        return repository.search(keyword, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Conversation> list(String assignedAgentId, Channel channel, String search, Pageable pageable) {
        Criteria base = Criteria.where("deleted").is(false);
        if (assignedAgentId != null && !assignedAgentId.isBlank()) {
            base = base.and("assignedAgentId").is(assignedAgentId);
        }
        if (channel != null) {
            base = base.and("channel").is(channel);
        }
        Query query = new Query(base);
        if (search != null && !search.isBlank()) {
            String regex = Pattern.quote(search.trim());
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("subject").regex(regex, "i"),
                    Criteria.where("lastMessagePreview").regex(regex, "i"),
                    Criteria.where("tags").regex(regex, "i")));
        }

        long total = mongoTemplate.count(query, Conversation.class);

        Pageable effective = pageable;
        if (pageable.getSort().isUnsorted()) {
            effective = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "lastMessageAt"));
        }
        query.with(effective);

        List<Conversation> content = mongoTemplate.find(query, Conversation.class);
        return PageableExecutionUtils.getPage(content, effective, () -> total);
    }

    // === DURUM ===

    @Override
    public Conversation changeStatus(String id, ConversationStatus newStatus) {
        Conversation conv = findByIdOrThrow(id);
        conv.setPreviousStatus(conv.getStatus());
        conv.setStatus(newStatus);
        conv.setStatusChangedAt(LocalDateTime.now());
        log.info("Conversation {} status: {} → {}", id, conv.getPreviousStatus(), newStatus);
        return repository.save(conv);
    }

    @Override
    public Conversation resolve(String id) {
        Conversation conv = changeStatus(id, ConversationStatus.RESOLVED);
        conv.setResolvedAt(LocalDateTime.now());
        return repository.save(conv);
    }

    @Override
    public Conversation archive(String id) {
        Conversation conv = changeStatus(id, ConversationStatus.ARCHIVED);
        conv.setArchivedAt(LocalDateTime.now());
        return repository.save(conv);
    }

    @Override
    public Conversation reopen(String id) {
        return changeStatus(id, ConversationStatus.OPEN);
    }

    // === MESAJ HOOK ===

    @Override
    public Conversation onNewMessage(String conversationId, String preview, String sender) {
        Conversation conv = findByIdOrThrow(conversationId);
        conv.addMessage(preview, sender);

        // İlk agent yanıtını kaydet
        if ("AGENT".equals(sender) && conv.getFirstResponseAt() == null) {
            conv.setFirstResponseAt(LocalDateTime.now());
        }

        // Durumu güncelle
        if ("LEAD".equals(sender) && conv.getStatus() == ConversationStatus.WAITING_REPLY) {
            conv.setStatus(ConversationStatus.AGENT_REPLY);
            conv.setStatusChangedAt(LocalDateTime.now());
        } else if ("AGENT".equals(sender) && conv.getStatus() == ConversationStatus.AGENT_REPLY) {
            conv.setStatus(ConversationStatus.WAITING_REPLY);
            conv.setStatusChangedAt(LocalDateTime.now());
        }

        return repository.save(conv);
    }

    @Override
    public Conversation markAsRead(String id) {
        Conversation conv = findByIdOrThrow(id);
        conv.markAsRead();
        return repository.save(conv);
    }

    // === ETİKET ===

    @Override
    public Conversation addTag(String id, String tag) {
        Conversation conv = findByIdOrThrow(id);
        if (conv.getTags() == null) conv.setTags(new HashSet<>());
        conv.getTags().add(tag.toUpperCase());
        return repository.save(conv);
    }

    @Override
    public Conversation removeTag(String id, String tag) {
        Conversation conv = findByIdOrThrow(id);
        if (conv.getTags() != null) conv.getTags().remove(tag.toUpperCase());
        return repository.save(conv);
    }

    // === PIN ===

    @Override
    public Conversation togglePin(String id) {
        Conversation conv = findByIdOrThrow(id);
        conv.setIsPinned(!conv.getIsPinned());
        return repository.save(conv);
    }

    // === AGENT ===

    @Override
    public Conversation assignToAgent(String id, String agentId) {
        Conversation conv = findByIdOrThrow(id);
        conv.setAssignedAgentId(agentId);
        log.info("Conversation {} assigned to agent {}", id, agentId);
        return repository.save(conv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> findUnreadByAgent(String agentId) {
        return repository.findUnreadByAgent(agentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> findPinnedByAgent(String agentId) {
        return repository.findByAssignedAgentIdAndIsPinnedTrueAndDeletedFalse(agentId);
    }

    // === İLİŞKİ ===

    @Override
    public Conversation linkTicket(String conversationId, String ticketId) {
        Conversation conv = findByIdOrThrow(conversationId);
        if (conv.getTicketIds() == null) conv.setTicketIds(new HashSet<>());
        conv.getTicketIds().add(ticketId);
        return repository.save(conv);
    }

    @Override
    public Conversation linkReminder(String conversationId, String reminderId) {
        Conversation conv = findByIdOrThrow(conversationId);
        if (conv.getReminderIds() == null) conv.setReminderIds(new HashSet<>());
        conv.getReminderIds().add(reminderId);
        return repository.save(conv);
    }

    @Override
    public Conversation linkAppointment(String conversationId, String appointmentId) {
        Conversation conv = findByIdOrThrow(conversationId);
        conv.setAppointmentId(appointmentId);
        return repository.save(conv);
    }

    // === İSTATİSTİK ===

    @Override
    @Transactional(readOnly = true)
    public long countAll() { return repository.countByDeletedFalse(); }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(ConversationStatus status) {
        return repository.countByStatusAndDeletedFalse(status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByChannel(Channel channel) {
        return repository.countByChannelAndDeletedFalse(channel);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadByAgent(String agentId) {
        return repository.countByAssignedAgentIdAndStatusAndDeletedFalse(
                agentId, ConversationStatus.AGENT_REPLY);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> findStaleConversations(int inactiveHours) {
        return repository.findStaleConversations(LocalDateTime.now().minusHours(inactiveHours));
    }

    // === PRIVATE ===

    private Conversation findByIdOrThrow(String id) {
        return repository.findById(id)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", id));
    }
}
