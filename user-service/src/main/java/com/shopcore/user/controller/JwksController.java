package com.shopcore.user.controller;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.RSAKey;
import com.shopcore.user.config.SecurityProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

@RestController
public class JwksController {

    private final RSAPublicKey publicKey;
    private final SecurityProperties properties;

    public JwksController(RSAPublicKey publicKey, SecurityProperties properties) {
        this.publicKey = publicKey;
        this.properties = properties;
    }

    @GetMapping("/.well-known/jwks.json")
    Map<String, Object> jwks() {
        Map<String, Object> key = new RSAKey.Builder(publicKey)
                .keyID(properties.jwt().keyId())
                .algorithm(JWSAlgorithm.RS256)
                .build()
                .toJSONObject();
        return Map.of("keys", List.of(key));
    }
}
