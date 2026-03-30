package com.lendiq.apigateway.service;

import com.lendiq.apigateway.dto.response.AuditTrailResponse;
import com.lendiq.apigateway.dto.response.DecisionResponse;
import com.lendiq.apigateway.dto.response.FraudFlagResponse;
import com.lendiq.apigateway.entity.Application;
import com.lendiq.apigateway.entity.Decision;
import com.lendiq.apigateway.entity.FraudEvent;
import com.lendiq.apigateway.exception.ResourceNotFoundException;
import com.lendiq.apigateway.mapper.DecisionMapper;
import com.lendiq.apigateway.mapper.FraudEventMapper;
import com.lendiq.apigateway.repository.ApplicationRepository;
import com.lendiq.apigateway.repository.DecisionRepository;
import com.lendiq.apigateway.repository.FraudEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final ApplicationRepository applicationRepository;
    private final DecisionRepository decisionRepository;
    private final FraudEventRepository fraudEventRepository;
    private final DecisionMapper decisionMapper;
    private final FraudEventMapper fraudEventMapper;

    @Override
    @Transactional(readOnly = true)
    public AuditTrailResponse getAuditTrail(UUID applicationId) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        Decision decision = decisionRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Decision", applicationId));

        DecisionResponse decisionResponse = decisionMapper.toResponse(decision);

        UUID applicantId = application.getApplicant().getId();
        List<FraudEvent> fraudEvents = fraudEventRepository
            .findByResolvedFalseOrderByDetectedAtDesc(PageRequest.of(0, 100))
            .getContent()
            .stream()
            .filter(fe -> fe.getApplicant().getId().equals(applicantId))
            .toList();

        List<FraudFlagResponse> fraudFlags = fraudEventMapper.toResponseList(fraudEvents);

        return new AuditTrailResponse(
            applicationId,
            applicantId,
            application.getCreatedAt(),
            application.getSourceChannel(),
            application.getAmount(),
            application.getPurpose(),
            decisionResponse,
            fraudFlags,
            application.getKafkaOffset() != null
                ? application.getKafkaOffset().toString() : null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public String exportAuditCsv(OffsetDateTime from, OffsetDateTime to) {
        // In production, this generates a CSV, uploads to S3,
        // and returns a pre-signed URL
        log.info("Audit CSV export requested [from={}, to={}]", from, to);
        return "https://lendiq-exports.s3.amazonaws.com/audit-" + UUID.randomUUID() + ".csv";
    }
}
