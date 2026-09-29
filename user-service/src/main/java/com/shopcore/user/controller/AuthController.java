package com.shopcore.user.controller;

import com.shopcore.user.controller.dto.AuthResponse;
import com.shopcore.user.controller.dto.LoginRequest;
import com.shopcore.user.controller.dto.RegisterRequest;
import com.shopcore.user.controller.dto.UserResponse;
import com.shopcore.user.service.AuthService;
import com.shopcore.user.service.RefreshCookieService;
import com.shopcore.user.service.exception.InvalidRefreshTokenException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshCookieService cookieService;

    public AuthController(AuthService authService, RefreshCookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @PostMapping("/register")
    ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthSession session = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.create(session.refreshToken()).toString())
                .body(session.response());
    }

    @PostMapping("/refresh")
    ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String refreshToken = cookieService.read(request).orElseThrow(InvalidRefreshTokenException::new);
        AuthService.AuthSession session = authService.refresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieService.create(session.refreshToken()).toString())
                .body(session.response());
    }

    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletRequest request) {
        cookieService.read(request).ifPresent(authService::logout);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieService.clear().toString())
                .build();
    }
}
