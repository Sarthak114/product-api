package com.zestindia.productapi.auth;

import com.zestindia.productapi.auth.dto.AuthResponse;
import com.zestindia.productapi.auth.dto.LoginRequest;
import com.zestindia.productapi.auth.dto.RefreshTokenRequest;
import com.zestindia.productapi.auth.dto.RegisterRequest;
import com.zestindia.productapi.common.exception.BadRequestException;
import com.zestindia.productapi.security.JwtService;
import com.zestindia.productapi.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserAccountRepository userAccountRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userAccountRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setEnabled(true);
        user.setCreatedOn(Instant.now());
        userAccountRepository.save(user);
        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserAccount user = userAccountRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadRequestException("User not found"));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(request.refreshToken());
        return issueAccessAndKeepRefresh(rotated.user(), rotated.refreshToken());
    }

    @Transactional
    public void logout(String username) {
        userAccountRepository.findByUsername(username)
                .ifPresent(user -> refreshTokenService.revokeAllForUser(user.getId()));
    }

    private AuthResponse issueTokens(UserAccount user) {
        String refreshToken = refreshTokenService.issue(user, null);
        return issueAccessAndKeepRefresh(user, refreshToken);
    }

    private AuthResponse issueAccessAndKeepRefresh(UserAccount user, String refreshToken) {
        UserPrincipal principal = UserPrincipal.from(user);
        String accessToken = jwtService.generateAccessToken(principal);
        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs(),
                user.getUsername(),
                user.getRole().name()
        );
    }
}
