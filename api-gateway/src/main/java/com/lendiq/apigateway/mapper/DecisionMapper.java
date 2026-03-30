package com.lendiq.apigateway.mapper;

import com.lendiq.apigateway.dto.response.ApplicationDetailResponse;
import com.lendiq.apigateway.dto.response.DecisionResponse;
import com.lendiq.apigateway.entity.Decision;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.*;

import java.util.Map;
import java.util.Collections;

@Mapper(componentModel = "spring")
public interface DecisionMapper {

    ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mapping(source = "application.id", target = "applicationId")
    @Mapping(source = "fraudProb", target = "fraudProbability")
    @Mapping(source = "lender.id", target = "lender.id")
    @Mapping(source = "lender.name", target = "lender.name")
    @Mapping(target = "shapValues", expression = "java(parseShapJson(decision.getShapJson()))")
    DecisionResponse toResponse(Decision decision);

    @Mapping(source = "outcome", target = "outcome")
    @Mapping(source = "finalScore", target = "finalScore")
    @Mapping(source = "modelVersion", target = "modelVersion")
    @Mapping(source = "decidedAt", target = "decidedAt")
    ApplicationDetailResponse.DecisionSummary toSummary(Decision decision);

    default Map<String, Double> parseShapJson(String shapJson) {
        if (shapJson == null || shapJson.isBlank()) return Collections.emptyMap();
        try {
            return OBJECT_MAPPER.readValue(shapJson, new TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
