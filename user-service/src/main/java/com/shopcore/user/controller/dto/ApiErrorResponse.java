package com.shopcore.user.controller.dto;

import java.time.Instant;

public record ApiErrorResponse(
        String code,
        String message,
        int status,
        String path,
        String correlationId,
        Instant timestamp
) {
}
