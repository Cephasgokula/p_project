package com.lendiq.apigateway.mapper;

import com.lendiq.apigateway.dto.response.ApplicationDetailResponse;
import com.lendiq.apigateway.dto.response.DecisionResponse;
import com.lendiq.apigateway.entity.Decision;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DecisionMapper {

    @Mapping(source = "application.id", target = "applicationId")
    @Mapping(source = "fraudProb", target = "fraudProbability")
    @Mapping(source = "lender.id", target = "lender.id")
    @Mapping(source = "lender.name", target = "lender.name")
    @Mapping(target = "shapValues", ignore = true)
    DecisionResponse toResponse(Decision decision);

    @Mapping(source = "outcome", target = "outcome")
    @Mapping(source = "finalScore", target = "finalScore")
    @Mapping(source = "modelVersion", target = "modelVersion")
    @Mapping(source = "decidedAt", target = "decidedAt")
    ApplicationDetailResponse.DecisionSummary toSummary(Decision decision);
}
