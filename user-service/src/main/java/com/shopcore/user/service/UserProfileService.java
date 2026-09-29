package com.shopcore.user.service;

import com.shopcore.user.controller.dto.UpdateProfileRequest;
import com.shopcore.user.controller.dto.UserResponse;
import com.shopcore.user.model.UserEntity;
import com.shopcore.user.repository.UserRepository;
import com.shopcore.user.service.exception.UserAlreadyExistsException;
import com.shopcore.user.service.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserProfileService {

    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrent(UUID userId) {
        return UserResponse.from(find(userId));
    }

    @Transactional
    public UserResponse updateCurrent(UUID userId, UpdateProfileRequest request) {
        UserEntity user = find(userId);
        String username = IdentityNormalizer.normalize(request.username());
        String email = IdentityNormalizer.normalize(request.email());
        if (userRepository.existsByUsernameAndIdNot(username, userId)
                || userRepository.existsByEmailAndIdNot(email, userId)) {
            throw new UserAlreadyExistsException();
        }
        user.updateProfile(username, email);
        userRepository.flush();
        return UserResponse.from(user);
    }

    private UserEntity find(UUID userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }
}
