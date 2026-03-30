package com.lendiq.apigateway.dto.response;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn) {}
