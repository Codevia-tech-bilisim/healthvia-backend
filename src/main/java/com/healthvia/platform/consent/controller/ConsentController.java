package com.healthvia.platform.consent.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthvia.platform.common.util.SecurityUtils;
import com.healthvia.platform.common.dto.ApiResponse;
import com.healthvia.platform.consent.dto.ConsentDto;
import com.healthvia.platform.consent.dto.ConsentRequestDto;
import com.healthvia.platform.consent.entity.Consent;
import com.healthvia.platform.consent.service.ConsentService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/consents")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN', 'AGENT')")
public class ConsentController {

    private final ConsentService consentService;

    @PostMapping("/request")
    public ApiResponse<ConsentDto> request(
            @Valid @RequestBody ConsentRequestDto req,
            HttpServletRequest httpReq) {
        String agentId = SecurityUtils.getCurrentUserId();
        String ip = httpReq.getRemoteAddr();
        String ua = httpReq.getHeader("User-Agent");
        Consent c = consentService.requestConsent(req, agentId, ip, ua);
        return ApiResponse.success(ConsentDto.fromEntity(c), "OTP gönderildi");
    }

    @PostMapping("/{id}/verify")
    public ApiResponse<ConsentDto> verify(
            @PathVariable String id,
            @RequestBody VerifyRequest body) {
        Consent c = consentService.verifyConsent(id, body.otp());
        return ApiResponse.success(ConsentDto.fromEntity(c), "Onay doğrulandı");
    }

    @GetMapping("/{id}")
    public ApiResponse<ConsentDto> getOne(@PathVariable String id) {
        return ApiResponse.success(ConsentDto.fromEntity(consentService.findByIdOrThrow(id)));
    }

    @GetMapping
    public ApiResponse<List<ConsentDto>> list(
            @RequestParam(required = false) String caseId,
            @RequestParam(required = false) String patientId) {
        List<Consent> list;
        if (caseId != null) list = consentService.findByCase(caseId);
        else if (patientId != null) list = consentService.findByPatient(patientId);
        else list = List.of();
        return ApiResponse.success(list.stream().map(ConsentDto::fromEntity).toList());
    }

    public record VerifyRequest(String otp) {}
}
