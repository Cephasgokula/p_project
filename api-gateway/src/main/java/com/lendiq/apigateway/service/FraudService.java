package com.lendiq.apigateway.service;

import com.lendiq.apigateway.dto.request.FraudFlagResolveRequest;
import com.lendiq.apigateway.dto.response.FraudFlagResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface FraudService {

    boolean checkVelocityFraud(String ipHash, String deviceFingerprint);

    void recordGnnFraudEvent(UUID applicantId, double fraudProbability, String ringId);

    Page<FraudFlagResponse> listFlags(String eventType, int page, int size);

    FraudFlagResponse getFlagById(UUID id);

    void resolveFlag(UUID id, FraudFlagResolveRequest request);
}
