package com.shopcore.user.repository;

import com.shopcore.user.model.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select token from RefreshTokenEntity token
            join fetch token.user user
            left join fetch user.roles
            where token.tokenHash = :tokenHash
            """)
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update RefreshTokenEntity token
            set token.revokedAt = :revokedAt
            where token.familyId = :familyId and token.revokedAt is null
            """)
    int revokeActiveByFamilyId(@Param("familyId") UUID familyId, @Param("revokedAt") Instant revokedAt);
}
