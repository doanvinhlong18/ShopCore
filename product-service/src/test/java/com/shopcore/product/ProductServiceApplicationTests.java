package com.shopcore.product;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = "debug=false")
class ProductServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
