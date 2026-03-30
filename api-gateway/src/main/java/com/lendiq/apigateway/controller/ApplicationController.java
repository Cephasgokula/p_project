package com.lendiq.apigateway.controller;

import com.lendiq.apigateway.dto.request.ApplicationFilterRequest;
import com.lendiq.apigateway.dto.request.ApplicationSubmitRequest;
import com.lendiq.apigateway.dto.request.BatchScoreRequest;
import com.lendiq.apigateway.dto.response.*;
import com.lendiq.apigateway.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public ApplicationSubmitResponse submit(
            @Valid @RequestBody ApplicationSubmitRequest request,
            @RequestHeader(value = "X-Source-Channel", defaultValue = "api") String sourceChannel) {
        return applicationService.submit(request, sourceChannel);
    }

    @GetMapping("/{id}")
    public ApplicationDetailResponse getById(
            @PathVariable UUID id,
            Authentication auth) {
        UUID callerId = extractApplicantId(auth);
        boolean isAdmin = isAdmin(auth);
        return applicationService.getById(id, callerId, isAdmin);
    }

    @GetMapping
    public PagedResponse<ApplicationDetailResponse> list(
            @ModelAttribute @Valid ApplicationFilterRequest filter,
            Authentication auth) {
        UUID callerId = extractApplicantId(auth);
        boolean isAdmin = isAdmin(auth);
        Page<ApplicationDetailResponse> page = applicationService.list(filter, callerId, isAdmin);
        return PagedResponse.from(page);
    }

    @GetMapping("/{id}/decision")
    public DecisionResponse getDecision(
            @PathVariable UUID id,
            Authentication auth) {
        UUID callerId = extractApplicantId(auth);
        return applicationService.getDecision(id, callerId);
    }

    @PatchMapping("/{id}/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(
            @PathVariable UUID id,
            Authentication auth) {
        UUID callerId = extractApplicantId(auth);
        applicationService.withdraw(id, callerId);
    }

    private UUID extractApplicantId(Authentication auth) {
        if (auth == null || auth.getName() == null) return null;
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
