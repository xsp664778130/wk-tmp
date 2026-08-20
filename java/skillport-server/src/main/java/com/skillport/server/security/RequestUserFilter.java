package com.skillport.server.security;

import com.skillport.server.config.SkillPortProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class RequestUserFilter extends OncePerRequestFilter {
    public static final String REQUEST_USER_ATTRIBUTE = "skillport.requestUser";
    private static final String GATEWAY_KEY_HEADER = "X-SkillPort-Gateway-Key";
    private static final String USER_ID_HEADER = "X-SkillPort-User-Id";
    private static final String USER_EMAIL_HEADER = "X-SkillPort-User-Email";

    private final SkillPortProperties properties;

    public RequestUserFilter(SkillPortProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/actuator/health") || path.equals("/api/v1/bridge/pair");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String suppliedKey = request.getHeader(GATEWAY_KEY_HEADER);
        String userId = request.getHeader(USER_ID_HEADER);
        if (!constantTimeEquals(properties.gatewayKey(), suppliedKey) || userId == null || userId.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid gateway identity");
            return;
        }
        request.setAttribute(REQUEST_USER_ATTRIBUTE,
                new RequestUser(userId.trim(), valueOrEmpty(request.getHeader(USER_EMAIL_HEADER))));
        filterChain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || expected.isBlank() || actual == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
