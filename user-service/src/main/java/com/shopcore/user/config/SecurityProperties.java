package com.shopcore.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "shopcore.security")
public record SecurityProperties(
        Jwt jwt,
        Refresh refresh,
        List<String> allowedOrigins
) {
    public record Jwt(
            String issuer,
            String audience,
            Duration accessTokenTtl,
            String keyId,
            Resource privateKeyLocation,
            Resource publicKeyLocation
    ) {
    }

    public record Refresh(
            Duration tokenTtl,
            String cookieName,
            String cookiePath,
            boolean cookieSecure,
            String sameSite
    ) {
    }
}
