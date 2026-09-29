package com.shopcore.user.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

public class OriginProtectionFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PATHS = Set.of(
            "/api/auth/refresh",
            "/api/auth/logout"
    );

    private final Set<String> allowedOrigins;
    private final SecurityErrorWriter errorWriter;

    public OriginProtectionFilter(Set<String> allowedOrigins, SecurityErrorWriter errorWriter) {
        this.allowedOrigins = allowedOrigins;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (HttpMethod.POST.matches(request.getMethod()) && PROTECTED_PATHS.contains(request.getRequestURI())) {
            String origin = request.getHeader("Origin");
            if (origin != null && !allowedOrigins.contains(origin)) {
                errorWriter.write(request, response, 403, "AUTH_ORIGIN_FORBIDDEN", "Origin is not allowed");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
