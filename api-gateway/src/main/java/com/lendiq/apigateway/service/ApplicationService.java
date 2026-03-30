package com.lendiq.apigateway.service;

import com.lendiq.apigateway.dto.request.ApplicationFilterRequest;
import com.lendiq.apigateway.dto.request.ApplicationSubmitRequest;
import com.lendiq.apigateway.dto.request.BatchScoreRequest;
import com.lendiq.apigateway.dto.response.ApplicationDetailResponse;
import com.lendiq.apigateway.dto.response.ApplicationSubmitResponse;
import com.lendiq.apigateway.dto.response.DecisionResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface ApplicationService {

    ApplicationSubmitResponse submit(ApplicationSubmitRequest request, String sourceChannel);

    ApplicationDetailResponse getById(UUID id, UUID callerApplicantId, boolean isAdmin);

    Page<ApplicationDetailResponse> list(ApplicationFilterRequest filter,
                                          UUID callerApplicantId, boolean isAdmin);

    DecisionResponse getDecision(UUID applicationId, UUID callerApplicantId);

    void withdraw(UUID applicationId, UUID callerApplicantId);

    UUID enqueueBatchScore(BatchScoreRequest request);
}
