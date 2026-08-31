package com.skillport.server.service;

import com.skillport.server.config.SkillPortProperties;
import com.skillport.server.domain.PasswordResetCodeEntity;
import com.skillport.server.domain.UserEntity;
import com.skillport.server.repository.PasswordResetCodeRepository;
import com.skillport.server.repository.UserRepository;
import com.skillport.server.repository.UserSessionRepository;
import com.skillport.server.security.TokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
public class PasswordResetStore {
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordResetCodeRepository resetCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final String hashSecret;

    public PasswordResetStore(UserRepository userRepository, UserSessionRepository sessionRepository,
                              PasswordResetCodeRepository resetCodeRepository, PasswordEncoder passwordEncoder,
                              TokenService tokenService, SkillPortProperties properties) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.resetCodeRepository = resetCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.hashSecret = properties.gatewayKey();
    }

    @Transactional
    public IssuedCode issueCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        UserEntity user = userRepository.findByEmailNormalized(normalizedEmail)
                .filter(value -> value.getWeComUserId() == null && "ACTIVE".equals(value.getStatus()))
                .orElse(null);
        if (user == null) return null;
        Instant now = Instant.now();
        PasswordResetCodeEntity latest = resetCodeRepository
                .findFirstByEmailNormalizedOrderByCreatedAtDesc(normalizedEmail).orElse(null);
        if (latest != null && latest.getCreatedAt().isAfter(now.minus(RESEND_COOLDOWN))) return null;

        String code = "%06d".formatted(SECURE_RANDOM.nextInt(1_000_000));
        resetCodeRepository.save(new PasswordResetCodeEntity(
                UUID.randomUUID().toString(), normalizedEmail, hash(normalizedEmail, code),
                now.plus(CODE_TTL), now));
        return new IssuedCode(user.getEmail(), user.getDisplayName(), code);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public void resetPassword(String email, String code, String newPassword) {
        validatePassword(newPassword);
        String normalizedEmail = normalizeEmail(email);
        UserEntity user = userRepository.findByEmailNormalized(normalizedEmail)
                .filter(value -> value.getWeComUserId() == null && "ACTIVE".equals(value.getStatus()))
                .orElseThrow(PasswordResetStore::invalidCode);
        PasswordResetCodeEntity resetCode = resetCodeRepository
                .findFirstByEmailNormalizedOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(PasswordResetStore::invalidCode);
        Instant now = Instant.now();
        if (resetCode.getConsumedAt() != null || !resetCode.getExpiresAt().isAfter(now)
                || resetCode.getAttempts() >= MAX_ATTEMPTS) throw invalidCode();
        if (!tokenService.matches(hashInput(normalizedEmail, code), resetCode.getCodeHash())) {
            resetCode.recordFailedAttempt();
            throw invalidCode();
        }
        resetCode.consume(now);
        user.changePassword(passwordEncoder.encode(newPassword), now);
        sessionRepository.revokeAllByOwnerId(user.getPublicId(), now);
    }

    private String hash(String email, String code) {
        return tokenService.sha256(hashInput(email, code));
    }

    private String hashInput(String email, String code) {
        return hashSecret + "\n" + email + "\n" + code.trim();
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码过长");
        }
    }

    private static ResponseStatusException invalidCode() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误或已过期");
    }

    public record IssuedCode(String email, String displayName, String code) {
    }
}
