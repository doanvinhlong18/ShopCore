package com.shopcore.user;

import com.shopcore.user.model.RoleEntity;
import com.shopcore.user.model.RoleName;
import com.shopcore.user.model.UserEntity;
import com.shopcore.user.repository.RefreshTokenRepository;
import com.shopcore.user.repository.RoleRepository;
import com.shopcore.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "debug=false")
@ActiveProfiles("test")
class UserPersistenceIntegrationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private RoleEntity userRole;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        userRole = roleRepository.findByName(RoleName.USER).orElseThrow();
    }

    @Test
    void persistsRelationsAndEnforcesUniqueIdentity() {
        UserEntity alice = save("alice", "alice@example.com");

        UserEntity reloaded = userRepository.findById(alice.getId()).orElseThrow();
        assertThat(reloaded.getRoles()).extracting(RoleEntity::getName).containsExactly(RoleName.USER);
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();

        UserEntity duplicate = new UserEntity(
                "alice",
                "other@example.com",
                passwordEncoder.encode("password123"),
                userRole
        );
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void optimisticLockPreventsLostProfileUpdate() {
        UserEntity user = save("alice", "alice@example.com");

        EntityManager firstManager = entityManagerFactory.createEntityManager();
        EntityManager secondManager = entityManagerFactory.createEntityManager();
        try {
            firstManager.getTransaction().begin();
            secondManager.getTransaction().begin();
            UserEntity firstCopy = firstManager.find(UserEntity.class, user.getId());
            UserEntity secondCopy = secondManager.find(UserEntity.class, user.getId());

            firstCopy.updateProfile("alice-one", "alice-one@example.com");
            firstManager.getTransaction().commit();

            secondCopy.updateProfile("alice-two", "alice-two@example.com");
            assertThatThrownBy(() -> secondManager.getTransaction().commit())
                    .isInstanceOf(RollbackException.class);
        } finally {
            if (firstManager.getTransaction().isActive()) {
                firstManager.getTransaction().rollback();
            }
            if (secondManager.getTransaction().isActive()) {
                secondManager.getTransaction().rollback();
            }
            firstManager.close();
            secondManager.close();
        }
    }

    private UserEntity save(String username, String email) {
        return userRepository.saveAndFlush(new UserEntity(
                username,
                email,
                passwordEncoder.encode("password123"),
                userRole
        ));
    }
}
