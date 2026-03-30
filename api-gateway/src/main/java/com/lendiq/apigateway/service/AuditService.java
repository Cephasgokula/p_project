package com.lendiq.apigateway.service;

import com.lendiq.apigateway.dto.response.AuditTrailResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface AuditService {

    AuditTrailResponse getAuditTrail(UUID applicationId);

    String exportAuditCsv(OffsetDateTime from, OffsetDateTime to);
}
