package com.shopcore.user.config;

import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class PemKeyLoader {

    private PemKeyLoader() {
    }

    static RSAPrivateKey readPrivateKey(Resource resource) {
        try {
            byte[] key = decode(resource, "PRIVATE KEY");
            return (RSAPrivateKey) KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(key));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load JWT private key from " + resource, exception);
        }
    }

    static RSAPublicKey readPublicKey(Resource resource) {
        try {
            byte[] key = decode(resource, "PUBLIC KEY");
            return (RSAPublicKey) KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(key));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load JWT public key from " + resource, exception);
        }
    }

    private static byte[] decode(Resource resource, String type) throws Exception {
        String pem;
        try (var input = resource.getInputStream()) {
            pem = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        String encoded = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(encoded);
    }
}
