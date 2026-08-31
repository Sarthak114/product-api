package com.zestindia.productapi.auth;

import com.zestindia.productapi.common.exception.TokenRefreshException;
import com.zestindia.productapi.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private UserAccount user;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties();
        properties.getJwt().setRefreshTokenExpirationMs(60_000L);
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, properties);

        user = new UserAccount();
        user.setId(1);
        user.setUsername("admin");
        user.setRole(Role.ADMIN);
    }

    @Test
    void issuePersistsHashedToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String raw = refreshTokenService.issue(user, "family-1");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken stored = captor.getValue();
        assertThat(raw).isNotBlank();
        assertThat(stored.getTokenHash()).isEqualTo(RefreshTokenService.hash(raw));
        assertThat(stored.getFamilyId()).isEqualTo("family-1");
        assertThat(stored.getUser()).isEqualTo(user);
        assertThat(stored.isRevoked()).isFalse();
    }

    @Test
    void rotateRevokesOldAndIssuesNew() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String raw = refreshTokenService.issue(user, "family-1");

        RefreshToken existing = new RefreshToken();
        existing.setUser(user);
        existing.setFamilyId("family-1");
        existing.setTokenHash(RefreshTokenService.hash(raw));
        existing.setExpiresAt(Instant.now().plusSeconds(60));
        existing.setRevoked(false);
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(raw))).thenReturn(Optional.of(existing));

        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(raw);

        assertThat(existing.isRevoked()).isTrue();
        assertThat(rotated.user()).isEqualTo(user);
        assertThat(rotated.refreshToken()).isNotEqualTo(raw);
    }

    @Test
    void rotateRevokesFamilyWhenTokenAlreadyUsed() {
        RefreshToken reused = new RefreshToken();
        reused.setUser(user);
        reused.setFamilyId("family-9");
        reused.setTokenHash(RefreshTokenService.hash("old-token"));
        reused.setExpiresAt(Instant.now().plusSeconds(60));
        reused.setRevoked(true);
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash("old-token")))
                .thenReturn(Optional.of(reused));

        assertThatThrownBy(() -> refreshTokenService.rotate("old-token"))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("reuse");
        verify(refreshTokenRepository).revokeFamily("family-9");
    }

    @Test
    void rotateRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("missing"))
                .isInstanceOf(TokenRefreshException.class);
    }
}
