package com.lendiq.apigateway.mapper;

import com.lendiq.apigateway.dto.request.LenderOnboardRequest;
import com.lendiq.apigateway.dto.request.LenderRulesUpdateRequest;
import com.lendiq.apigateway.dto.response.LenderResponse;
import com.lendiq.apigateway.entity.Lender;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LenderMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    Lender toEntity(LenderOnboardRequest request);

    LenderResponse toResponse(Lender lender);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateFromRequest(LenderRulesUpdateRequest request, @MappingTarget Lender lender);
}
