package com.zestindia.productapi.auth;

import com.zestindia.productapi.common.exception.TokenRefreshException;
import com.zestindia.productapi.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AppProperties appProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, AppProperties appProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.appProperties = appProperties;
    }

    @Transactional
    public String issue(UserAccount user, String familyId) {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(rawToken));
        token.setFamilyId(familyId == null ? UUID.randomUUID().toString() : familyId);
        token.setExpiresAt(Instant.now().plusMillis(appProperties.getJwt().getRefreshTokenExpirationMs()));
        token.setRevoked(false);
        token.setCreatedOn(Instant.now());
        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Transactional
    public RotatedToken rotate(String rawRefreshToken) {
        String hash = hash(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new TokenRefreshException("Refresh token is invalid"));

        if (existing.isRevoked() || existing.isExpired()) {
            refreshTokenRepository.revokeFamily(existing.getFamilyId());
            throw new TokenRefreshException("Refresh token reuse or expiry detected; family has been revoked");
        }

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        String next = issue(existing.getUser(), existing.getFamilyId());
        return new RotatedToken(existing.getUser(), next);
    }

    @Transactional
    public void revokeAllForUser(Integer userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public record RotatedToken(UserAccount user, String refreshToken) {
    }
}
