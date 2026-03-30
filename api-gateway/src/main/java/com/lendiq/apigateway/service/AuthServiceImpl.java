package com.lendiq.apigateway.service;

import com.lendiq.apigateway.config.AppProperties;
import com.lendiq.apigateway.dto.response.AuthResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AppProperties appProperties;
    private final PasswordEncoder passwordEncoder;

    // In production, store users in a database table.
    // This in-memory map is for development/demo only.
    private final Map<String, UserRecord> users = new ConcurrentHashMap<>(Map.of(
        "admin@lendiq.local", new UserRecord(
            "admin@lendiq.local",
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", // password: admin123
            List.of("ROLE_ADMIN")
        ),
        "applicant@demo.local", new UserRecord(
            "applicant@demo.local",
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", // password: admin123
            List.of("ROLE_APPLICANT")
        )
    ));

    private record UserRecord(String email, String passwordHash, List<String> roles) {}

    @Override
    public AuthResponse login(String email, String password) {
        UserRecord user = users.get(email);
        if (user == null || !passwordEncoder.matches(password, user.passwordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }
        return generateTokens(user.email(), user.roles());
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        SecretKey key = Keys.hmacShaKeyFor(appProperties.security().jwtSecret().getBytes());
        Claims claims = Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(refreshToken).getPayload();

        String email = claims.getSubject();
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);

        return generateTokens(email, roles != null ? roles : List.of());
    }

    private AuthResponse generateTokens(String subject, List<String> roles) {
        SecretKey key = Keys.hmacShaKeyFor(appProperties.security().jwtSecret().getBytes());
        long expiryHours = appProperties.security().jwtExpiryHours();
        long refreshDays = appProperties.security().refreshExpiryDays();

        Instant now = Instant.now();

        String accessToken = Jwts.builder()
            .subject(subject)
            .claim("roles", roles)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expiryHours, ChronoUnit.HOURS)))
            .signWith(key)
            .compact();

        String refreshToken = Jwts.builder()
            .subject(subject)
            .claim("roles", roles)
            .claim("type", "refresh")
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(refreshDays, ChronoUnit.DAYS)))
            .signWith(key)
            .compact();

        return new AuthResponse(accessToken, refreshToken, expiryHours * 3600);
    }

    /** Register a new user (called by ApplicantController on registration). */
    public void registerUser(String email, String rawPassword, List<String> roles) {
        users.put(email, new UserRecord(email, passwordEncoder.encode(rawPassword), roles));
    }
}
