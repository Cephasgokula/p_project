package com.lendiq.apigateway.controller;

import com.lendiq.apigateway.dto.request.LenderOnboardRequest;
import com.lendiq.apigateway.dto.request.LenderRulesUpdateRequest;
import com.lendiq.apigateway.dto.response.LenderResponse;
import com.lendiq.apigateway.dto.response.LenderStatsResponse;
import com.lendiq.apigateway.dto.response.PagedResponse;
import com.lendiq.apigateway.service.LenderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lenders")
@RequiredArgsConstructor
public class LenderController {

    private final LenderService lenderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LenderResponse onboard(@Valid @RequestBody LenderOnboardRequest request) {
        return lenderService.onboard(request);
    }

    @GetMapping
    public PagedResponse<LenderResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<LenderResponse> result = lenderService.list(page, size);
        return PagedResponse.from(result);
    }

    @GetMapping("/{id}")
    public LenderResponse getById(@PathVariable UUID id) {
        return lenderService.getById(id);
    }

    @PutMapping("/{id}/rules")
    public LenderResponse updateRules(
            @PathVariable UUID id,
            @Valid @RequestBody LenderRulesUpdateRequest request) {
        return lenderService.updateRules(id, request);
    }

    @PatchMapping("/{id}/pause")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pause(@PathVariable UUID id) {
        lenderService.pause(id);
    }

    @GetMapping("/{id}/stats")
    public LenderStatsResponse getStats(@PathVariable UUID id) {
        return lenderService.getStats(id);
    }
}
