package com.shopcore.user.repository;

import com.shopcore.user.model.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);

    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByUsernameOrEmail(String username, String email);

    @Override
    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findById(UUID id);
}
