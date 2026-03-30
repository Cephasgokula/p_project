package com.lendiq.apigateway.service;

import com.lendiq.apigateway.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(String email, String password);
    AuthResponse refresh(String refreshToken);
}
