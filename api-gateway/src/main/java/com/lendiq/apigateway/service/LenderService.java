package com.lendiq.apigateway.service;

import com.lendiq.apigateway.dto.request.LenderOnboardRequest;
import com.lendiq.apigateway.dto.request.LenderRulesUpdateRequest;
import com.lendiq.apigateway.dto.response.LenderResponse;
import com.lendiq.apigateway.dto.response.LenderStatsResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface LenderService {

    LenderResponse onboard(LenderOnboardRequest request);

    Page<LenderResponse> list(int page, int size);

    LenderResponse getById(UUID id);

    LenderResponse updateRules(UUID id, LenderRulesUpdateRequest request);

    void pause(UUID id);

    LenderStatsResponse getStats(UUID id);
}
