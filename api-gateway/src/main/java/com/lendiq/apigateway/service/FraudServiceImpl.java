package com.lendiq.apigateway.service;

import com.lendiq.apigateway.config.AppProperties;
import com.lendiq.apigateway.dto.request.FraudFlagResolveRequest;
import com.lendiq.apigateway.dto.response.FraudFlagResponse;
import com.lendiq.apigateway.dsa.SlidingWindow;
import com.lendiq.apigateway.entity.Applicant;
import com.lendiq.apigateway.entity.FraudEvent;
import com.lendiq.apigateway.exception.ResourceNotFoundException;
import com.lendiq.apigateway.mapper.FraudEventMapper;
import com.lendiq.apigateway.repository.ApplicantRepository;
import com.lendiq.apigateway.repository.FraudEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FraudServiceImpl implements FraudService {

    private final SlidingWindow slidingWindow;
    private final FraudEventRepository fraudEventRepository;
    private final ApplicantRepository applicantRepository;
    private final FraudEventMapper fraudEventMapper;
    private final AppProperties appProperties;

    @Override
    public boolean checkVelocityFraud(String ipHash, String deviceFingerprint) {
        if (ipHash == null || deviceFingerprint == null) return false;

        return slidingWindow.isVelocityFraud(
            ipHash,
            deviceFingerprint,
            appProperties.fraud().velocityThreshold(),
            appProperties.fraud().velocityWindowSecs()
        );
    }

    @Override
    @Transactional
    public void recordGnnFraudEvent(UUID applicantId, double fraudProbability, String ringId) {
        Applicant applicant = applicantRepository.findById(applicantId).orElse(null);
        if (applicant == null) {
            log.warn("Cannot record fraud event — applicant not found: {}", applicantId);
            return;
        }

        FraudEvent event = new FraudEvent();
        event.setApplicant(applicant);
        event.setEventType(ringId != null ? "gnn_ring" : "gnn_score");
        event.setFraudProb(BigDecimal.valueOf(fraudProbability));
        event.setRingId(ringId);
        event.setResolved(false);

        fraudEventRepository.save(event);
        log.info("GNN fraud event recorded [applicantId={}, fraudProb={}, ringId={}]",
            applicantId, fraudProbability, ringId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FraudFlagResponse> listFlags(String eventType, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<FraudEvent> events;

        if (eventType != null && !eventType.isBlank()) {
            events = fraudEventRepository.findByEventTypeAndResolvedFalse(eventType, pageable);
        } else {
            events = fraudEventRepository.findByResolvedFalseOrderByDetectedAtDesc(pageable);
        }

        return events.map(fraudEventMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FraudFlagResponse getFlagById(UUID id) {
        FraudEvent event = fraudEventRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FraudEvent", id));
        return fraudEventMapper.toResponse(event);
    }

    @Override
    @Transactional
    public void resolveFlag(UUID id, FraudFlagResolveRequest request) {
        int updated = fraudEventRepository.resolveFlag(id);
        if (updated == 0) {
            throw new ResourceNotFoundException("FraudEvent", id);
        }
        log.info("Fraud flag resolved [id={}, by={}]", id, request.reviewBy());
    }
}
