package com.shopcore.user.repository;

import com.shopcore.user.model.RoleEntity;
import com.shopcore.user.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(RoleName name);
}
