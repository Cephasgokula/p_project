package com.lendiq.apigateway.service;

import com.lendiq.apigateway.config.AppProperties;
import com.lendiq.apigateway.dto.request.ApplicationFilterRequest;
import com.lendiq.apigateway.dto.request.ApplicationSubmitRequest;
import com.lendiq.apigateway.dto.request.BatchScoreRequest;
import com.lendiq.apigateway.dto.response.ApplicationDetailResponse;
import com.lendiq.apigateway.dto.response.ApplicationSubmitResponse;
import com.lendiq.apigateway.dto.response.DecisionResponse;
import com.lendiq.apigateway.entity.Applicant;
import com.lendiq.apigateway.entity.Application;
import com.lendiq.apigateway.entity.Decision;
import com.lendiq.apigateway.exception.InsufficientPermissionException;
import com.lendiq.apigateway.exception.ResourceNotFoundException;
import com.lendiq.apigateway.exception.VelocityFraudException;
import com.lendiq.apigateway.kafka.event.LoanApplicationEvent;
import com.lendiq.apigateway.kafka.producer.ApplicationEventProducer;
import com.lendiq.apigateway.mapper.ApplicationMapper;
import com.lendiq.apigateway.mapper.DecisionMapper;
import com.lendiq.apigateway.repository.ApplicantRepository;
import com.lendiq.apigateway.repository.ApplicationRepository;
import com.lendiq.apigateway.repository.DecisionRepository;
import com.lendiq.apigateway.repository.spec.ApplicationSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final ApplicantRepository applicantRepository;
    private final DecisionRepository decisionRepository;
    private final ApplicationMapper applicationMapper;
    private final DecisionMapper decisionMapper;
    private final ApplicationEventProducer eventProducer;
    private final FraudService fraudService;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public ApplicationSubmitResponse submit(ApplicationSubmitRequest request, String sourceChannel) {
        // Look up the applicant
        Applicant applicant = applicantRepository.findById(request.applicantId())
            .orElseThrow(() -> new ResourceNotFoundException("Applicant", request.applicantId()));

        // Velocity fraud pre-check
        if (fraudService.checkVelocityFraud(applicant.getIpHash(), request.deviceFingerprint())) {
            throw new VelocityFraudException();
        }

        // Create application entity
        Application application = applicationMapper.toEntity(request);
        application.setApplicant(applicant);
        application.setSourceChannel(sourceChannel != null ? sourceChannel : "api");
        application = applicationRepository.save(application);

        // Compute DTI ratio
        BigDecimal dti = BigDecimal.ZERO;
        if (applicant.getIncome() != null && applicant.getIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debt = applicant.getExistingDebt() != null ? applicant.getExistingDebt() : BigDecimal.ZERO;
            dti = debt.divide(applicant.getIncome(), 4, RoundingMode.HALF_UP);
        }

        // Publish to Kafka for scoring pipeline
        LoanApplicationEvent event = new LoanApplicationEvent(
            UUID.randomUUID(),
            application.getId(),
            applicant.getId(),
            0L,
            request.amount(),
            request.termMonths(),
            request.purpose(),
            applicant.getIncome(),
            applicant.getAge() != null ? applicant.getAge() : 0,
            applicant.getEmploymentMonths() != null ? applicant.getEmploymentMonths() : 0,
            applicant.getExistingDebt(),
            applicant.getCreditBreauScore() != null ? applicant.getCreditBreauScore() : 0,
            dti,
            applicant.getIpHash(),
            request.deviceFingerprint(),
            false,
            Instant.now(),
            sourceChannel != null ? sourceChannel : "api"
        );

        eventProducer.publish(event);

        log.info("Application submitted [id={}, applicant={}, amount={}]",
            application.getId(), applicant.getId(), request.amount());

        return new ApplicationSubmitResponse(
            application.getId(),
            "pending",
            null,
            null,
            null,
            null,
            null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationDetailResponse getById(UUID id, UUID callerApplicantId, boolean isAdmin) {
        Application application;
        if (isAdmin) {
            application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));
        } else {
            application = applicationRepository.findByIdAndApplicantId(id, callerApplicantId)
                .orElseThrow(() -> new ResourceNotFoundException("Application", id));
        }

        ApplicationDetailResponse response = applicationMapper.toDetailResponse(application);

        // Attach decision summary if available
        Decision decision = decisionRepository.findByApplicationId(id).orElse(null);
        if (decision != null) {
            ApplicationDetailResponse.DecisionSummary summary = decisionMapper.toSummary(decision);
            response = new ApplicationDetailResponse(
                response.id(), response.applicantId(), response.amount(),
                response.termMonths(), response.purpose(), response.status(),
                response.sourceChannel(), response.createdAt(), summary
            );
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ApplicationDetailResponse> list(ApplicationFilterRequest filter,
                                                 UUID callerApplicantId, boolean isAdmin) {
        ApplicationFilterRequest effectiveFilter = filter;
        if (!isAdmin) {
            // Non-admin can only see their own applications
            effectiveFilter = new ApplicationFilterRequest(
                callerApplicantId,
                filter.status(), filter.from(), filter.to(),
                filter.channel(), filter.page(), filter.size()
            );
        }

        PageRequest pageable = PageRequest.of(
            effectiveFilter.page(),
            effectiveFilter.size(),
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return applicationRepository
            .findAll(ApplicationSpec.withFilters(effectiveFilter), pageable)
            .map(applicationMapper::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public DecisionResponse getDecision(UUID applicationId, UUID callerApplicantId) {
        // Verify ownership
        applicationRepository.findByIdAndApplicantId(applicationId, callerApplicantId)
            .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        Decision decision = decisionRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Decision", applicationId));

        return decisionMapper.toResponse(decision);
    }

    @Override
    @Transactional
    public void withdraw(UUID applicationId, UUID callerApplicantId) {
        Application application = applicationRepository
            .findByIdAndApplicantId(applicationId, callerApplicantId)
            .orElseThrow(() -> new ResourceNotFoundException("Application", applicationId));

        if (!"pending".equals(application.getStatus())) {
            throw new InsufficientPermissionException(
                "Only pending applications can be withdrawn");
        }

        application.setStatus("withdrawn");
        applicationRepository.save(application);
        log.info("Application withdrawn [id={}]", applicationId);
    }

    @Override
    public UUID enqueueBatchScore(BatchScoreRequest request) {
        UUID jobId = UUID.randomUUID();
        log.info("Batch score job enqueued [jobId={}, count={}]",
            jobId, request.applicationIds().size());
        // In production, this would enqueue each application for scoring
        return jobId;
    }
}
