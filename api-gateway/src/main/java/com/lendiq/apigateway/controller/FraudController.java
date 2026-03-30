package com.lendiq.apigateway.controller;

import com.lendiq.apigateway.dto.request.FraudFlagResolveRequest;
import com.lendiq.apigateway.dto.response.FraudFlagResponse;
import com.lendiq.apigateway.dto.response.PagedResponse;
import com.lendiq.apigateway.service.FraudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud")
@RequiredArgsConstructor
public class FraudController {

    private final FraudService fraudService;

    @GetMapping("/flags")
    public PagedResponse<FraudFlagResponse> listFlags(
            @RequestParam(required = false) String eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<FraudFlagResponse> result = fraudService.listFlags(eventType, page, size);
        return PagedResponse.from(result);
    }

    @GetMapping("/flags/{id}")
    public FraudFlagResponse getFlagById(@PathVariable UUID id) {
        return fraudService.getFlagById(id);
    }

    @PatchMapping("/flags/{id}/resolve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolveFlag(
            @PathVariable UUID id,
            @Valid @RequestBody FraudFlagResolveRequest request) {
        fraudService.resolveFlag(id, request);
    }
}
