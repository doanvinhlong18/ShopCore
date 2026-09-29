package com.shopcore.user.service;

import com.shopcore.user.controller.dto.AuthResponse;
import com.shopcore.user.controller.dto.LoginRequest;
import com.shopcore.user.controller.dto.RegisterRequest;
import com.shopcore.user.controller.dto.UserResponse;
import com.shopcore.user.model.RoleEntity;
import com.shopcore.user.model.RoleName;
import com.shopcore.user.model.UserEntity;
import com.shopcore.user.model.UserStatus;
import com.shopcore.user.repository.RoleRepository;
import com.shopcore.user.repository.UserRepository;
import com.shopcore.user.service.exception.InvalidCredentialsException;
import com.shopcore.user.service.exception.InvalidRefreshTokenException;
import com.shopcore.user.service.exception.UserAlreadyExistsException;
import com.shopcore.user.service.exception.UserNotActiveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = IdentityNormalizer.normalize(request.username());
        String email = IdentityNormalizer.normalize(request.email());
        if (userRepository.existsByUsername(username) || userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException();
        }

        RoleEntity userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("Default USER role is missing"));
        UserEntity user = new UserEntity(
                username,
                email,
                passwordEncoder.encode(request.password()),
                userRole
        );
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new UserAlreadyExistsException();
        }
        log.info("User registered userId={}", user.getId());
        return UserResponse.from(user);
    }

    @Transactional
    public AuthSession login(LoginRequest request) {
        String login = IdentityNormalizer.normalize(request.login());
        UserEntity user = userRepository.findByUsernameOrEmail(login, login)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UserNotActiveException();
        }

        JwtTokenService.AccessToken accessToken = jwtTokenService.issue(user);
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issueNewFamily(user);
        log.info("User login succeeded userId={}", user.getId());
        return session(user, accessToken, refreshToken);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthSession refresh(String rawRefreshToken) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(rawRefreshToken);
        JwtTokenService.AccessToken accessToken = jwtTokenService.issue(rotated.user());
        return session(rotated.user(), accessToken, rotated.token());
    }

    public void logout(String rawRefreshToken) {
        refreshTokenService.logout(rawRefreshToken);
        log.info("Refresh token logout completed");
    }

    private AuthSession session(
            UserEntity user,
            JwtTokenService.AccessToken accessToken,
            RefreshTokenService.IssuedRefreshToken refreshToken
    ) {
        return new AuthSession(
                new AuthResponse(
                        accessToken.value(),
                        accessToken.expiresInSeconds(),
                        UserResponse.from(user)
                ),
                refreshToken.value()
        );
    }

    public record AuthSession(AuthResponse response, String refreshToken) {
    }
}
