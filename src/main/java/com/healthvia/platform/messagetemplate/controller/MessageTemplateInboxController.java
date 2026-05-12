// messagetemplate/controller/MessageTemplateInboxController.java
package com.healthvia.platform.messagetemplate.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.messagetemplate.dto.MessageTemplateDto;
import com.healthvia.platform.messagetemplate.entity.MessageTemplate;
import com.healthvia.platform.messagetemplate.service.MessageTemplateService;

import lombok.RequiredArgsConstructor;

/**
 * Inbox-facing template lookup.
 *
 * The agent dashboard's ConversationPanel composer fetches templates as
 * a flat array via {@code GET /api/v1/message-templates?language=tr}. The
 * existing admin controller lives at {@code /api/v1/templates}, returns
 * a paginated page, and only allows ROLE_ADMIN — so for an agent in the
 * inbox it would 404 (path mismatch) and then 403 (role mismatch).
 *
 * Rather than break the admin surface, this controller is a thin
 * read-only adapter: flat list, agent-accessible, optional language
 * filter. CRUD/render endpoints stay on {@link MessageTemplateController}.
 */
@RestController
@RequestMapping("/api/v1/message-templates")
@PreAuthorize("hasAnyRole('SUPERADMIN','ADMIN','CEO','AGENT')")
@RequiredArgsConstructor
public class MessageTemplateInboxController {

    private static final int MAX_INBOX_TEMPLATES = 500;

    private final MessageTemplateService templateService;

    @GetMapping
    public ApiResponse<List<MessageTemplateDto>> list(
            @RequestParam(required = false) String language) {
        List<MessageTemplate> templates = (language == null || language.isBlank())
                ? templateService.findAll(PageRequest.of(0, MAX_INBOX_TEMPLATES)).getContent()
                : templateService.findByLanguage(language);
        return ApiResponse.success(
                templates.stream().map(MessageTemplateDto::fromEntityBasic).toList());
    }
}
