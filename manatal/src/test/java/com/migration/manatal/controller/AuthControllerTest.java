package com.migration.manatal.controller;

import com.migration.manatal.dto.LoginRequest;
import com.migration.manatal.security.JwtUtils;
import com.migration.manatal.security.SecurityProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtUtils jwtUtils;

    private final SecurityProperties properties = new SecurityProperties(
            "dev-secret-key-que-tem-pelo-menos-256-bits-para-hmac-sha", 24, "admin", "admin123");

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(properties, passwordEncoder, jwtUtils);
    }

    @Test
    void shouldReturnTokenOnValidCredentials() {
        when(jwtUtils.generateToken("admin")).thenReturn("jwt-token");

        var response = controller.login(new LoginRequest("admin", "admin123"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        var body = (java.util.Map<String, Object>) response.getBody();
        assertEquals("jwt-token", body.get("token"));
        assertEquals("24h", body.get("expiresIn"));
    }

    @Test
    void shouldRejectWrongUsername() {
        var response = controller.login(new LoginRequest("root", "admin123"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        var body = (java.util.Map<String, Object>) response.getBody();
        assertEquals("Credenciais inválidas", body.get("error"));
    }

    @Test
    void shouldRejectWrongPassword() {
        var response = controller.login(new LoginRequest("admin", "wrong"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        var body = (java.util.Map<String, Object>) response.getBody();
        assertEquals("Credenciais inválidas", body.get("error"));
    }

    @Test
    void shouldAcceptBcryptHashInProperties() {
        String hash = passwordEncoder.encode("supersecret");
        var hashedController = new AuthController(
                new SecurityProperties("dev-secret-key-que-tem-pelo-menos-256-bits-para-hmac-sha",
                        24, "admin", hash),
                passwordEncoder, jwtUtils);
        when(jwtUtils.generateToken("admin")).thenReturn("jwt-token");

        var ok = hashedController.login(new LoginRequest("admin", "supersecret"));
        assertEquals(HttpStatus.OK, ok.getStatusCode());

        var bad = hashedController.login(new LoginRequest("admin", "wrong"));
        assertEquals(HttpStatus.UNAUTHORIZED, bad.getStatusCode());
        assertNotNull(ok.getBody());
        assertFalse(ok.getBody().toString().contains("wrong"));
        assertTrue(bad.getBody() != null);
    }
}
