package com.lendiq.apigateway.config;

import com.lendiq.apigateway.security.AdminIpFilter;
import com.lendiq.apigateway.security.ApiKeyAuthFilter;
import com.lendiq.apigateway.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthFilter jwtFilter,
                                           ApiKeyAuthFilter apiKeyFilter,
                                           AdminIpFilter adminIpFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(adminIpFilter, JwtAuthFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/health").permitAll()
                .requestMatchers(POST, "/api/v1/applications").hasAnyRole("APPLICANT", "ADMIN")
                .requestMatchers(GET, "/api/v1/applications/**").hasAnyRole("APPLICANT", "ADMIN")
                .requestMatchers("/api/v1/lenders/**").hasAnyRole("LENDER", "ADMIN")
                .requestMatchers("/api/v1/audit/**").hasAnyRole("ADMIN", "REGULATOR")
                .requestMatchers("/api/v1/fraud/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/models/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
