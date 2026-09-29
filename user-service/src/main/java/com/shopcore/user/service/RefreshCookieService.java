package com.shopcore.user.service;

import com.shopcore.user.config.SecurityProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
public class RefreshCookieService {

    private final SecurityProperties properties;

    public RefreshCookieService(SecurityProperties properties) {
        this.properties = properties;
    }

    public Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> properties.refresh().cookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    public ResponseCookie create(String value) {
        return ResponseCookie.from(properties.refresh().cookieName(), value)
                .httpOnly(true)
                .secure(properties.refresh().cookieSecure())
                .sameSite(properties.refresh().sameSite())
                .path(properties.refresh().cookiePath())
                .maxAge(properties.refresh().tokenTtl())
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(properties.refresh().cookieName(), "")
                .httpOnly(true)
                .secure(properties.refresh().cookieSecure())
                .sameSite(properties.refresh().sameSite())
                .path(properties.refresh().cookiePath())
                .maxAge(0)
                .build();
    }
}
