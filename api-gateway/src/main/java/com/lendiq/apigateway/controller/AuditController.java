package com.lendiq.apigateway.controller;

import com.lendiq.apigateway.dto.response.AuditTrailResponse;
import com.lendiq.apigateway.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/{applicationId}")
    public AuditTrailResponse getAuditTrail(@PathVariable UUID applicationId) {
        return auditService.getAuditTrail(applicationId);
    }

    @GetMapping("/export")
    public Map<String, String> exportAuditCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        String url = auditService.exportAuditCsv(from, to);
        return Map.of("downloadUrl", url);
    }
}
