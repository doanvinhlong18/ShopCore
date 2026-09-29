package com.shopcore.user;

import com.shopcore.user.controller.dto.LoginRequest;
import com.shopcore.user.controller.dto.RegisterRequest;
import com.shopcore.user.repository.RefreshTokenRepository;
import com.shopcore.user.repository.UserRepository;
import com.shopcore.user.service.AuthService;
import com.shopcore.user.service.exception.InvalidRefreshTokenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "debug=false")
@ActiveProfiles("test")
class RefreshTokenConcurrencyIntegrationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private final java.util.concurrent.ExecutorService executor = Executors.newFixedThreadPool(2);

    @BeforeEach
    void prepareUser() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        authService.register(new RegisterRequest("alice", "alice@example.com", "password123"));
    }

    @AfterEach
    void shutdownExecutor() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void onlyOneConcurrentRefreshSucceeds() throws Exception {
        String refreshToken = authService.login(new LoginRequest("alice", "password123")).refreshToken();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> refresh = () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
                authService.refresh(refreshToken);
                return true;
            } catch (InvalidRefreshTokenException exception) {
                return false;
            }
        };

        var results = List.of(executor.submit(refresh), executor.submit(refresh));
        assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        long successes = 0;
        for (var result : results) {
            if (result.get(10, TimeUnit.SECONDS)) {
                successes++;
            }
        }
        assertThat(successes).isEqualTo(1);
    }
}
