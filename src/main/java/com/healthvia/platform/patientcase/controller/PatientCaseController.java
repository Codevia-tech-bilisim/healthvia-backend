package com.healthvia.platform.patientcase.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.common.dto.PageResponse;
import com.healthvia.platform.patientcase.dto.PatientCaseDto;
import com.healthvia.platform.patientcase.entity.PatientCase;
import com.healthvia.platform.patientcase.entity.PatientCase.CaseStatus;
import com.healthvia.platform.patientcase.service.PatientCaseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'AGENT', 'CEO')")
public class PatientCaseController {

    private final PatientCaseService caseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'AGENT')")
    public ApiResponse<PatientCaseDto> createFromLead(@RequestParam String leadId) {
        PatientCase c = caseService.createFromLead(leadId);
        return ApiResponse.success(PatientCaseDto.fromEntity(c), "Case oluşturuldu");
    }

    @GetMapping
    public ApiResponse<PageResponse<PatientCaseDto>> list(
            @RequestParam(required = false) List<CaseStatus> status,
            @RequestParam(required = false) String assignedAgentId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<PatientCase> result;
        if (keyword != null && !keyword.isBlank()) {
            result = caseService.search(keyword, pageable);
        } else if (status != null && !status.isEmpty()) {
            result = caseService.findByStatus(status, pageable);
        } else if (assignedAgentId != null) {
            result = caseService.findByAgent(assignedAgentId, pageable);
        } else {
            result = caseService.findAll(pageable);
        }
        PageResponse<PatientCaseDto> body = PageResponse.<PatientCaseDto>builder()
            .content(result.getContent().stream().map(PatientCaseDto::fromEntity).toList())
            .totalElements(result.getTotalElements())
            .totalPages(result.getTotalPages())
            .currentPage(result.getNumber())
            .pageSize(result.getSize())
            .first(result.isFirst())
            .last(result.isLast())
            .empty(result.isEmpty())
            .build();
        return ApiResponse.success(body);
    }

    @GetMapping("/{id}")
    public ApiResponse<PatientCaseDto> getOne(@PathVariable String id) {
        return ApiResponse.success(PatientCaseDto.fromEntity(caseService.findByIdOrThrow(id)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'AGENT')")
    public ApiResponse<PatientCaseDto> changeStatus(
            @PathVariable String id,
            @RequestParam CaseStatus status) {
        return ApiResponse.success(
            PatientCaseDto.fromEntity(caseService.changeStatus(id, status)),
            "Durum güncellendi");
    }

    @GetMapping("/{id}/timeline")
    public ApiResponse<List<Object>> timeline(@PathVariable String id) {
        // Full timeline — implementation depends on event store; return empty skeleton for now.
        caseService.findByIdOrThrow(id);
        return ApiResponse.success(List.of());
    }
}
