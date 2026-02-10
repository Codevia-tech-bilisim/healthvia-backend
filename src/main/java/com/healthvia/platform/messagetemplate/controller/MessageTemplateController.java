// messagetemplate/controller/MessageTemplateController.java
package com.healthvia.platform.messagetemplate.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.messagetemplate.dto.MessageTemplateDto;
import com.healthvia.platform.messagetemplate.entity.MessageTemplate;
import com.healthvia.platform.messagetemplate.entity.MessageTemplate.TemplateCategory;
import com.healthvia.platform.messagetemplate.service.MessageTemplateService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/templates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class MessageTemplateController {

    private final MessageTemplateService templateService;

    // === CRUD ===

    @PostMapping
    public ApiResponse<MessageTemplateDto> create(@Valid @RequestBody MessageTemplate request) {
        MessageTemplate created = templateService.create(request);
        return ApiResponse.success(MessageTemplateDto.fromEntity(created), "Şablon oluşturuldu");
    }

    @PutMapping("/{id}")
    public ApiResponse<MessageTemplateDto> update(
            @PathVariable String id, @RequestBody MessageTemplate request) {
        MessageTemplate updated = templateService.update(id, request);
        return ApiResponse.success(MessageTemplateDto.fromEntity(updated), "Şablon güncellendi");
    }

    @GetMapping("/{id}")
    public ApiResponse<MessageTemplateDto> getById(@PathVariable String id) {
        return templateService.findById(id)
                .map(MessageTemplateDto::fromEntity)
                .map(ApiResponse::success)
                .orElse(ApiResponse.error("Şablon bulunamadı"));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        templateService.delete(id, SecurityUtils.getCurrentUserId());
        return ApiResponse.success("Şablon silindi");
    }

    // === LİSTELEME ===

    @GetMapping
    public ApiResponse<Page<MessageTemplateDto>> getAll(@PageableDefault(size = 50) Pageable pageable) {
        Page<MessageTemplate> templates = templateService.findAll(pageable);
        return ApiResponse.success(templates.map(MessageTemplateDto::fromEntityBasic));
    }

    @GetMapping("/language/{language}")
    public ApiResponse<List<MessageTemplateDto>> getByLanguage(@PathVariable String language) {
        List<MessageTemplate> templates = templateService.findByLanguage(language);
        return ApiResponse.success(templates.stream().map(MessageTemplateDto::fromEntityBasic).toList());
    }

    @GetMapping("/category/{category}/language/{language}")
    public ApiResponse<List<MessageTemplateDto>> getByCategoryAndLanguage(
            @PathVariable TemplateCategory category,
            @PathVariable String language) {
        List<MessageTemplate> templates = templateService.findByCategoryAndLanguage(category, language);
        return ApiResponse.success(templates.stream().map(MessageTemplateDto::fromEntityBasic).toList());
    }

    @GetMapping("/channel/{channel}/language/{language}")
    public ApiResponse<List<MessageTemplateDto>> getByChannelAndLanguage(
            @PathVariable String channel,
            @PathVariable String language) {
        List<MessageTemplate> templates = templateService.findByChannelAndLanguage(channel, language);
        return ApiResponse.success(templates.stream().map(MessageTemplateDto::fromEntityBasic).toList());
    }

    @GetMapping("/search")
    public ApiResponse<Page<MessageTemplateDto>> search(
            @RequestParam String keyword,
            @PageableDefault(size = 50) Pageable pageable) {
        return ApiResponse.success(templateService.search(keyword, pageable)
                .map(MessageTemplateDto::fromEntityBasic));
    }

    // === RENDER ===

    @PostMapping("/{id}/render")
    public ApiResponse<RenderResponse> render(
            @PathVariable String id,
            @RequestBody Map<String, String> values) {
        String rendered = templateService.render(id, values);
        RenderResponse response = new RenderResponse();
        response.setRenderedContent(rendered);
        return ApiResponse.success(response);
    }

    // === YÖNETİM ===

    @PatchMapping("/{id}/toggle-active")
    public ApiResponse<MessageTemplateDto> toggleActive(@PathVariable String id) {
        MessageTemplate updated = templateService.toggleActive(id);
        String msg = Boolean.TRUE.equals(updated.getIsActive()) ? "Şablon aktifleştirildi" : "Şablon devre dışı bırakıldı";
        return ApiResponse.success(MessageTemplateDto.fromEntity(updated), msg);
    }

    @lombok.Data
    public static class RenderResponse {
        private String renderedContent;
    }
}
