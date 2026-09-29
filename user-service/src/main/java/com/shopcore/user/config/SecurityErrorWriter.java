package com.shopcore.user.config;

import com.shopcore.user.controller.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;
import tools.jackson.databind.ObjectMapper;

public class SecurityErrorWriter {

    private final ObjectMapper objectMapper;

    public SecurityErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            int status,
            String code,
            String message
    ) throws IOException {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        String correlationId = value == null ? "unknown" : value.toString();
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ApiErrorResponse(
                code,
                message,
                status,
                request.getRequestURI(),
                correlationId,
                Instant.now()
        ));
    }
}
