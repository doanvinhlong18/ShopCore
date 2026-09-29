package com.shopcore.user.controller.dto;

import com.shopcore.user.model.RoleEntity;
import com.shopcore.user.model.UserEntity;
import com.shopcore.user.model.UserStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(
        UUID id,
        String username,
        String email,
        UserStatus status,
        Set<String> roles,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserResponse from(UserEntity user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(RoleEntity::getName)
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getStatus(),
                roleNames,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
