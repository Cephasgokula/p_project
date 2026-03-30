package com.lendiq.apigateway.controller;

import com.lendiq.apigateway.dto.request.ApplicantRegisterRequest;
import com.lendiq.apigateway.dto.response.ApplicantResponse;
import com.lendiq.apigateway.entity.Applicant;
import com.lendiq.apigateway.repository.ApplicantRepository;
import com.lendiq.apigateway.service.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@RestController
@RequestMapping("/api/v1/applicants")
@RequiredArgsConstructor
public class ApplicantController {

    private final ApplicantRepository applicantRepository;
    private final AuthServiceImpl authService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicantResponse register(@Valid @RequestBody ApplicantRegisterRequest request) {
        String panHash = sha256(request.panNumber());

        Applicant applicant = Applicant.builder()
            .fullName(request.fullName())
            .panHash(panHash)
            .income(request.income())
            .age(request.age())
            .employmentMonths(request.employmentMonths())
            .existingDebt(request.existingDebt() != null ? request.existingDebt() : BigDecimal.ZERO)
            .creditBreauScore(request.creditBureauScore())
            .deviceFp(request.deviceFingerprint())
            .ipHash(request.ipAddress() != null ? sha256(request.ipAddress()) : null)
            .build();

        applicant = applicantRepository.save(applicant);

        // Register user credentials for JWT auth
        authService.registerUser(request.email(), request.password(), List.of("ROLE_APPLICANT"));

        return new ApplicantResponse(
            applicant.getId(),
            applicant.getFullName(),
            applicant.getIncome(),
            applicant.getAge(),
            applicant.getEmploymentMonths(),
            applicant.getExistingDebt(),
            applicant.getCreditBreauScore(),
            applicant.getCreatedAt()
        );
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
