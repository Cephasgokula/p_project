package com.lendiq.apigateway.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Component
public class AdminIpFilter extends OncePerRequestFilter {

    @Value("${lendiq.security.admin-cidrs}")
    private List<String> allowedCidrs;

    private static final List<String> ADMIN_PATHS = List.of("/api/v1/models", "/api/v1/fraud");

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        boolean isAdminPath = ADMIN_PATHS.stream().anyMatch(req.getRequestURI()::startsWith);
        if (!isAdminPath) {
            chain.doFilter(req, res);
            return;
        }

        String callerIp = getClientIp(req);
        if (allowedCidrs.stream().noneMatch(cidr -> matchesCidr(callerIp, cidr))) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN, "IP not in admin allowlist");
            return;
        }
        chain.doFilter(req, res);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private boolean matchesCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            InetAddress cidrAddress = InetAddress.getByName(parts[0]);
            int prefixLength = Integer.parseInt(parts[1]);

            byte[] cidrBytes = cidrAddress.getAddress();
            byte[] ipBytes = InetAddress.getByName(ip).getAddress();

            if (cidrBytes.length != ipBytes.length) return false;

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (cidrBytes[i] != ipBytes[i]) return false;
            }
            if (remainingBits > 0) {
                int mask = 0xFF << (8 - remainingBits);
                return (cidrBytes[fullBytes] & mask) == (ipBytes[fullBytes] & mask);
            }
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
