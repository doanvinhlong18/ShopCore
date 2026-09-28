package com.shopcore.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = "debug=false")
class UserServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
