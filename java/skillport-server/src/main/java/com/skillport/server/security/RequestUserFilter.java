package com.skillport.server.security;

import com.skillport.server.config.SkillPortProperties;
import com.skillport.server.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class RequestUserFilter extends OncePerRequestFilter {
    public static final String REQUEST_USER_ATTRIBUTE = "skillport.requestUser";
    private static final String GATEWAY_KEY_HEADER = "X-SkillPort-Gateway-Key";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final SkillPortProperties properties;
    private final AuthService authService;

    public RequestUserFilter(SkillPortProperties properties, AuthService authService) {
        this.properties = properties;
        this.authService = authService;
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
        if (!constantTimeEquals(properties.gatewayKey(), suppliedKey)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid gateway identity");
            return;
        }

        if (isPublicAuthPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        String token = authorization != null && authorization.startsWith(BEARER_PREFIX)
                ? authorization.substring(BEARER_PREFIX.length()).trim()
                : "";
        RequestUser user;
        try {
            user = authService.authenticate(token);
        } catch (ResponseStatusException exception) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid or expired session");
            return;
        }
        request.setAttribute(REQUEST_USER_ATTRIBUTE, user);
        filterChain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || expected.isBlank() || actual == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean isPublicAuthPath(String path) {
        return path.equals("/api/v1/auth/register") || path.equals("/api/v1/auth/login");
    }
}
