package com.lendiq.apigateway.mapper;

import com.lendiq.apigateway.dto.response.FraudFlagResponse;
import com.lendiq.apigateway.entity.FraudEvent;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface FraudEventMapper {

    @Mapping(source = "applicant.id", target = "applicantId")
    @Mapping(source = "fraudProb", target = "fraudProbability")
    FraudFlagResponse toResponse(FraudEvent event);

    List<FraudFlagResponse> toResponseList(List<FraudEvent> events);
}
