package com.lendiq.apigateway.service;

import com.lendiq.apigateway.dto.request.LenderOnboardRequest;
import com.lendiq.apigateway.dto.request.LenderRulesUpdateRequest;
import com.lendiq.apigateway.dto.response.LenderResponse;
import com.lendiq.apigateway.dto.response.LenderStatsResponse;
import com.lendiq.apigateway.dsa.IntervalTree;
import com.lendiq.apigateway.entity.Lender;
import com.lendiq.apigateway.exception.ResourceNotFoundException;
import com.lendiq.apigateway.mapper.LenderMapper;
import com.lendiq.apigateway.repository.LenderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LenderServiceImpl implements LenderService {

    private final LenderRepository lenderRepository;
    private final LenderMapper lenderMapper;
    private final IntervalTree intervalTree;

    @Override
    @Transactional
    public LenderResponse onboard(LenderOnboardRequest request) {
        Lender lender = lenderMapper.toEntity(request);
        lender = lenderRepository.save(lender);

        // Rebuild Interval Tree with the new lender
        List<Lender> activeLenders = lenderRepository.findByActiveTrue();
        intervalTree.rebuild(activeLenders);

        log.info("Lender onboarded [id={}, name={}]", lender.getId(), lender.getName());
        return lenderMapper.toResponse(lender);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LenderResponse> list(int page, int size) {
        return lenderRepository.findAll(PageRequest.of(page, size))
            .map(lenderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public LenderResponse getById(UUID id) {
        Lender lender = lenderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lender", id));
        return lenderMapper.toResponse(lender);
    }

    @Override
    @Transactional
    public LenderResponse updateRules(UUID id, LenderRulesUpdateRequest request) {
        Lender lender = lenderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lender", id));

        lenderMapper.updateFromRequest(request, lender);
        lender = lenderRepository.save(lender);

        // Rebuild Interval Tree with updated rules
        List<Lender> activeLenders = lenderRepository.findByActiveTrue();
        intervalTree.rebuild(activeLenders);

        log.info("Lender rules updated [id={}, name={}]", lender.getId(), lender.getName());
        return lenderMapper.toResponse(lender);
    }

    @Override
    @Transactional
    public void pause(UUID id) {
        Lender lender = lenderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lender", id));

        lender.setActive(false);
        lenderRepository.save(lender);

        // Remove from Interval Tree
        List<Lender> activeLenders = lenderRepository.findByActiveTrue();
        intervalTree.rebuild(activeLenders);

        log.info("Lender paused [id={}]", id);
    }

    @Override
    @Transactional(readOnly = true)
    public LenderStatsResponse getStats(UUID id) {
        Lender lender = lenderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Lender", id));

        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);
        Instant since = thirtyDaysAgo.toInstant();

        List<Object[]> stats = lenderRepository.findLenderStatsSince(since);
        for (Object[] row : stats) {
            UUID lenderId = (UUID) row[0];
            if (lenderId.equals(id)) {
                long referrals = ((Number) row[2]).longValue();
                long approvals = ((Number) row[3]).longValue();
                double rate = referrals > 0 ? (double) approvals / referrals : 0.0;

                return new LenderStatsResponse(
                    lenderId,
                    (String) row[1],
                    referrals,
                    approvals,
                    rate,
                    BigDecimal.ZERO,
                    thirtyDaysAgo,
                    OffsetDateTime.now(ZoneOffset.UTC)
                );
            }
        }

        return new LenderStatsResponse(
            id, lender.getName(), 0, 0, 0.0,
            BigDecimal.ZERO, thirtyDaysAgo, OffsetDateTime.now(ZoneOffset.UTC)
        );
    }
}
