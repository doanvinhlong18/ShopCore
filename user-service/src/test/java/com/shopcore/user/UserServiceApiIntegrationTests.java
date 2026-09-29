package com.shopcore.user;

import com.shopcore.user.config.SecurityProperties;
import com.shopcore.user.repository.RefreshTokenRepository;
import com.shopcore.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "debug=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserServiceApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private SecurityProperties securityProperties;

    @BeforeEach
    void cleanDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void completesAuthenticationAndProfileLifecycle() throws Exception {
        register("Alice", "Alice@Example.com", "password123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        MvcResult login = login("ALICE", "password123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().httpOnly("shopcore_refresh", true))
                .andExpect(cookie().secure("shopcore_refresh", false))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("SameSite=Strict")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, org.hamcrest.Matchers.containsString("Path=/api/auth")))
                .andReturn();

        String accessToken = json(login, "accessToken");
        Cookie initialRefresh = requiredCookie(login, "shopcore_refresh");

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));

        mockMvc.perform(put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"Alice.New","email":"new@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice.new"))
                .andExpect(jsonPath("$.email").value("new@example.com"));

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(initialRefresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(cookie().exists("shopcore_refresh"))
                .andReturn();
        Cookie rotatedRefresh = requiredCookie(refreshed, "shopcore_refresh");
        assertThat(rotatedRefresh.getValue()).isNotEqualTo(initialRefresh.getValue());

        mockMvc.perform(post("/api/auth/logout").cookie(rotatedRefresh))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("shopcore_refresh", 0));

        mockMvc.perform(post("/api/auth/refresh").cookie(rotatedRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_TOKEN_INVALID"));
    }

    @Test
    void rejectsDuplicateInvalidAndSecuritySensitivePayloads() throws Exception {
        register("alice", "alice@example.com", "password123").andExpect(status().isCreated());

        register("ALICE", "other@example.com", "password123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_ALREADY_EXISTS"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bob","email":"bob@example.com","password":"password123","roles":["ADMIN"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));

        login("alice", "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_INVALID_CREDENTIALS"));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_MISSING"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void rejectsInactiveUsersAndUntrustedOrigins() throws Exception {
        register("alice", "alice@example.com", "password123").andExpect(status().isCreated());
        jdbcTemplate.update("UPDATE users SET status = 'LOCKED' WHERE username = 'alice'");

        login("alice", "password123")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_NOT_ACTIVE"));

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Origin", "https://evil.example")
                        .cookie(new Cookie("shopcore_refresh", "not-a-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_ORIGIN_FORBIDDEN"));
    }

    @Test
    void detectsRefreshTokenReplayAndRevokesFamily() throws Exception {
        register("alice", "alice@example.com", "password123").andExpect(status().isCreated());
        MvcResult login = login("alice", "password123").andExpect(status().isOk()).andReturn();
        Cookie initial = requiredCookie(login, "shopcore_refresh");

        MvcResult refresh = mockMvc.perform(post("/api/auth/refresh").cookie(initial))
                .andExpect(status().isOk())
                .andReturn();
        Cookie replacement = requiredCookie(refresh, "shopcore_refresh");

        mockMvc.perform(post("/api/auth/refresh").cookie(initial))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_TOKEN_INVALID"));

        mockMvc.perform(post("/api/auth/refresh").cookie(replacement))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REFRESH_TOKEN_INVALID"));
    }

    @Test
    void rejectsJwtWithWrongAudience() throws Exception {
        register("alice", "alice@example.com", "password123").andExpect(status().isCreated());
        UUID userId = userRepository.findByUsernameOrEmail("alice", "alice")
                .orElseThrow()
                .getId();
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(securityProperties.jwt().issuer())
                .audience(List.of("another-api"))
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of("USER"))
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(securityProperties.jwt().keyId())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_TOKEN_INVALID"));
    }

    @Test
    void publishesPublicJwksWithoutPrivateMaterial() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
                .andExpect(jsonPath("$.keys[0].kid").value(securityProperties.jwt().keyId()))
                .andExpect(jsonPath("$.keys[0].d").doesNotExist());
    }

    private org.springframework.test.web.servlet.ResultActions register(
            String username,
            String email,
            String password
    ) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","email":"%s","password":"%s"}
                        """.formatted(username, email, password)));
    }

    private org.springframework.test.web.servlet.ResultActions login(String login, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"login":"%s","password":"%s"}
                        """.formatted(login, password)));
    }

    private String json(MvcResult result, String field) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsByteArray()).get(field).asText();
    }

    private Cookie requiredCookie(MvcResult result, String name) {
        Cookie cookie = result.getResponse().getCookie(name);
        assertThat(cookie).isNotNull();
        return cookie;
    }
}
