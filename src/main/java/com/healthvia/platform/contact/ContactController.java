package com.healthvia.platform.contact;

import com.healthvia.platform.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contact")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Contact", description = "Public contact form endpoint")
public class ContactController {

    private final ContactEmailService contactEmailService;

    @PostMapping
    @Operation(summary = "Send a contact message", description = "Sends the user's inquiry to the support email address")
    public ResponseEntity<ApiResponse<Void>> sendContactMessage(
            @Valid @RequestBody ContactRequest request) {

        log.info("Contact form submission from: {} <{}>", request.getFullName(), request.getEmail());

        contactEmailService.sendContactEmail(request);

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Your message has been sent successfully. We will get back to you soon."));
    }
}
