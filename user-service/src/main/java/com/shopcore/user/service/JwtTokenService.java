package com.shopcore.user.service;

import com.shopcore.user.config.SecurityProperties;
import com.shopcore.user.model.RoleEntity;
import com.shopcore.user.model.UserEntity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtTokenService(JwtEncoder jwtEncoder, SecurityProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public AccessToken issue(UserEntity user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.jwt().accessTokenTtl());
        List<String> roles = user.getRoles().stream()
                .map(RoleEntity::getName)
                .map(Enum::name)
                .sorted()
                .toList();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(properties.jwt().keyId())
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .audience(List.of(properties.jwt().audience()))
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("roles", roles)
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, properties.jwt().accessTokenTtl().toSeconds());
    }

    public record AccessToken(String value, long expiresInSeconds) {
    }
}
