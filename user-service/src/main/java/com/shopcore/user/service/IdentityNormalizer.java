package com.shopcore.user.service;

import java.util.Locale;

public final class IdentityNormalizer {

    private IdentityNormalizer() {
    }

    public static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
