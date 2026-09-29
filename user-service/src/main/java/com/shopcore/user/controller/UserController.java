package com.shopcore.user.controller;

import com.shopcore.user.controller.dto.UpdateProfileRequest;
import com.shopcore.user.controller.dto.UserResponse;
import com.shopcore.user.service.UserProfileService;
import com.shopcore.user.service.exception.InvalidAccessTokenException;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    UserResponse getCurrent(@AuthenticationPrincipal Jwt jwt) {
        return userProfileService.getCurrent(subject(jwt));
    }

    @PutMapping("/me")
    UserResponse updateCurrent(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return userProfileService.updateCurrent(subject(jwt), request);
    }

    private UUID subject(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new InvalidAccessTokenException();
        }
    }
}
