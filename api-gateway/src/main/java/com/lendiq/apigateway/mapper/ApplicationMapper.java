package com.lendiq.apigateway.mapper;

import com.lendiq.apigateway.dto.request.ApplicationSubmitRequest;
import com.lendiq.apigateway.dto.response.ApplicationDetailResponse;
import com.lendiq.apigateway.dto.response.ApplicationSubmitResponse;
import com.lendiq.apigateway.entity.Application;
import com.lendiq.apigateway.entity.Decision;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {DecisionMapper.class})
public interface ApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "pending")
    @Mapping(target = "kafkaOffset", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "applicant", ignore = true)
    @Mapping(target = "sourceChannel", constant = "api")
    @Mapping(source = "amount", target = "amount")
    Application toEntity(ApplicationSubmitRequest request);

    @Mapping(source = "applicant.id", target = "applicantId")
    @Mapping(target = "decision", ignore = true)
    ApplicationDetailResponse toDetailResponse(Application application);

    @Mapping(source = "applicant.id", target = "applicantId")
    @Mapping(target = "decision", ignore = true)
    ApplicationDetailResponse toListItem(Application application);

    @Mapping(source = "application.id", target = "applicationId")
    @Mapping(source = "application.status", target = "status")
    @Mapping(source = "decision.outcome", target = "outcome")
    @Mapping(source = "decision.finalScore", target = "finalScore")
    @Mapping(source = "decision.processingMs", target = "processingMs")
    @Mapping(target = "lender", ignore = true)
    @Mapping(target = "decisionPath", ignore = true)
    ApplicationSubmitResponse toSubmitResponse(Application application, Decision decision);
}
