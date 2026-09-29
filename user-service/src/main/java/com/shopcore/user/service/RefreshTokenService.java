package com.shopcore.user.service;

import com.shopcore.user.config.SecurityProperties;
import com.shopcore.user.model.RefreshTokenEntity;
import com.shopcore.user.model.UserEntity;
import com.shopcore.user.model.UserStatus;
import com.shopcore.user.repository.RefreshTokenRepository;
import com.shopcore.user.service.exception.InvalidRefreshTokenException;
import com.shopcore.user.service.exception.UserNotActiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repository;
    private final SecurityProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository repository,
            SecurityProperties properties,
            Clock clock
    ) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedRefreshToken issueNewFamily(UserEntity user) {
        return issue(user, UUID.randomUUID());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RotatedRefreshToken rotate(String rawToken) {
        Instant now = clock.instant();
        RefreshTokenEntity current = repository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.getRevokedAt() != null) {
            if (current.getReplacedByTokenId() != null) {
                repository.revokeActiveByFamilyId(current.getFamilyId(), now);
                log.warn("Refresh token reuse detected; token family revoked for userId={}",
                        current.getUser().getId());
            }
            throw new InvalidRefreshTokenException();
        }

        if (!current.getExpiresAt().isAfter(now)) {
            current.revoke(now);
            throw new InvalidRefreshTokenException();
        }

        UserEntity user = current.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            current.revoke(now);
            throw new UserNotActiveException();
        }

        IssuedRefreshToken replacement = issue(user, current.getFamilyId());
        repository.flush();
        current.replaceWith(replacement.id(), now);
        return new RotatedRefreshToken(user, replacement);
    }

    @Transactional
    public void logout(String rawToken) {
        repository.findByTokenHashForUpdate(hash(rawToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    private IssuedRefreshToken issue(UserEntity user, UUID familyId) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(properties.refresh().tokenTtl());
        UUID id = UUID.randomUUID();
        String rawToken = generateRawToken();
        repository.save(new RefreshTokenEntity(id, user, hash(rawToken), familyId, expiresAt, now));
        return new IssuedRefreshToken(id, rawToken, expiresAt);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record IssuedRefreshToken(UUID id, String value, Instant expiresAt) {
    }

    public record RotatedRefreshToken(UserEntity user, IssuedRefreshToken token) {
    }
}
