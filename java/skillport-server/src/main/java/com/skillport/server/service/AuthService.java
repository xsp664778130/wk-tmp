package com.skillport.server.service;

import com.skillport.server.config.SkillPortProperties;
import com.skillport.server.domain.UserEntity;
import com.skillport.server.domain.UserSessionEntity;
import com.skillport.server.repository.UserRepository;
import com.skillport.server.repository.UserSessionRepository;
import com.skillport.server.security.RequestUser;
import com.skillport.server.security.TokenService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private static final Duration DEFAULT_SESSION_TTL = Duration.ofDays(30);
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Duration sessionTtl;

    public AuthService(UserRepository userRepository, UserSessionRepository sessionRepository,
                       PasswordEncoder passwordEncoder, TokenService tokenService,
                       SkillPortProperties properties) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.sessionTtl = properties.sessionTtl() == null ? DEFAULT_SESSION_TTL : properties.sessionTtl();
    }

    @Transactional
    public SessionGrant register(String email, String displayName, String password) {
        validatePasswordBytes(password);
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailNormalized(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已经注册");
        }

        Instant now = Instant.now();
        UserEntity user = new UserEntity(UUID.randomUUID().toString(), email.trim(), normalizedEmail,
                displayName.trim(), passwordEncoder.encode(password), now);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已经注册", exception);
        }
        return createSession(user, now);
    }

    @Transactional
    public SessionGrant login(String email, String password) {
        validatePasswordBytes(password);
        UserEntity user = userRepository.findByEmailNormalized(normalizeEmail(email)).orElse(null);
        if (user == null) {
            passwordEncoder.encode(password);
            throw invalidCredentials();
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash()) || !"ACTIVE".equals(user.getStatus())) {
            throw invalidCredentials();
        }
        return createSession(user, Instant.now());
    }

    @Transactional
    public SessionGrant loginWithWeCom(String corpId, String userId, String displayName) {
        String normalizedCorpId = requiredIdentityPart(corpId, "企业 ID");
        String normalizedUserId = requiredIdentityPart(userId, "企业成员 ID");
        UserEntity existing = userRepository
                .findByWeComCorpIdAndWeComUserId(normalizedCorpId, normalizedUserId)
                .orElse(null);
        if (existing != null) {
            if (!"ACTIVE".equals(existing.getStatus())) throw unauthorized();
            return createSession(existing, Instant.now());
        }

        Instant now = Instant.now();
        String identityHash = tokenService.sha256(normalizedCorpId + "\n" + normalizedUserId);
        String syntheticEmail = "wecom-" + identityHash.substring(0, 32) + "@accounts.skillport.local";
        String safeDisplayName = displayName == null || displayName.isBlank() ? "企业微信成员" : displayName.trim();
        if (safeDisplayName.length() > 120) safeDisplayName = safeDisplayName.substring(0, 120);
        UserEntity user = new UserEntity(UUID.randomUUID().toString(), syntheticEmail, syntheticEmail,
                safeDisplayName, passwordEncoder.encode(tokenService.randomToken(32)),
                normalizedCorpId, normalizedUserId, now);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "企业微信账户正在登录，请重试", exception);
        }
        return createSession(user, now);
    }

    @Transactional(readOnly = true)
    public RequestUser authenticate(String rawToken) {
        AuthenticatedUser user = requireAuthenticatedUser(rawToken);
        return new RequestUser(user.id(), user.email(), user.displayName());
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser currentUser(String rawToken) {
        return requireAuthenticatedUser(rawToken);
    }

    @Transactional(readOnly = true)
    public UserProfile profile(String ownerId) {
        UserEntity user = requireActiveUser(ownerId);
        return profileOf(user);
    }

    @Transactional
    public UserProfile updateProfile(String ownerId, String displayName) {
        String normalizedName = displayName == null ? "" : displayName.trim();
        if (normalizedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "显示名称不能为空");
        }
        UserEntity user = requireActiveUser(ownerId);
        user.updateDisplayName(normalizedName, Instant.now());
        return profileOf(user);
    }

    @Transactional
    public void changePassword(String ownerId, String currentPassword, String newPassword) {
        validatePasswordBytes(currentPassword);
        validatePasswordBytes(newPassword);
        UserEntity user = requireActiveUser(ownerId);
        if (user.getWeComUserId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "企业微信账户没有独立密码");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前密码不正确");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码不能与当前密码相同");
        }
        Instant now = Instant.now();
        user.changePassword(passwordEncoder.encode(newPassword), now);
        sessionRepository.revokeAllByOwnerId(ownerId, now);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        sessionRepository.findByTokenHashAndRevokedAtIsNull(tokenService.sha256(rawToken))
                .ifPresent(session -> session.revoke(Instant.now()));
    }

    private SessionGrant createSession(UserEntity user, Instant now) {
        String rawToken = tokenService.randomToken(32);
        Instant expiresAt = now.plus(sessionTtl);
        sessionRepository.save(new UserSessionEntity(tokenService.sha256(rawToken), user.getPublicId(), expiresAt, now));
        return new SessionGrant(rawToken, expiresAt,
                new AuthenticatedUser(user.getPublicId(), publicEmail(user), user.getDisplayName()));
    }

    private AuthenticatedUser requireAuthenticatedUser(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw unauthorized();
        UserSessionEntity session = sessionRepository
                .findByTokenHashAndRevokedAtIsNull(tokenService.sha256(rawToken))
                .filter(value -> value.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(AuthService::unauthorized);
        UserEntity user = userRepository.findByPublicId(session.getOwnerId())
                .filter(value -> "ACTIVE".equals(value.getStatus()))
                .orElseThrow(AuthService::unauthorized);
        return new AuthenticatedUser(user.getPublicId(), publicEmail(user), user.getDisplayName());
    }

    private UserEntity requireActiveUser(String ownerId) {
        return userRepository.findByPublicId(ownerId)
                .filter(value -> "ACTIVE".equals(value.getStatus()))
                .orElseThrow(AuthService::unauthorized);
    }

    private static UserProfile profileOf(UserEntity user) {
        return new UserProfile(user.getPublicId(), publicEmail(user), user.getDisplayName(),
                user.getWeComUserId() == null);
    }

    private static String publicEmail(UserEntity user) {
        return user.getWeComUserId() == null ? user.getEmail() : "企业微信账户";
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String requiredIdentityPart(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + "不能为空");
        }
        return value.trim();
    }

    private static void validatePasswordBytes(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码过长");
        }
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "邮箱或密码错误");
    }

    private static ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效");
    }

    public record AuthenticatedUser(String id, String email, String displayName) {
    }

    public record UserProfile(String id, String email, String displayName, boolean passwordEnabled) {
    }

    public record SessionGrant(String token, Instant expiresAt, AuthenticatedUser user) {
    }
}
