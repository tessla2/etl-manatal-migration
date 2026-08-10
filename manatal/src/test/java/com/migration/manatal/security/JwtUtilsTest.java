package com.migration.manatal.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    private final SecurityProperties properties = new SecurityProperties(
            "dev-secret-key-que-tem-pelo-menos-256-bits-para-hmac-sha", 1, "admin", "admin123");

    private final JwtUtils jwtUtils = new JwtUtils(properties);

    @Test
    void shouldGenerateValidToken() {
        String token = jwtUtils.generateToken("admin");

        assertTrue(jwtUtils.isTokenValid(token));
        assertEquals("admin", jwtUtils.extractUsername(token));
    }

    @Test
    void shouldRejectGarbageToken() {
        assertFalse(jwtUtils.isTokenValid("not-a-jwt"));
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtUtils.generateToken("admin");
        String tampered = token.substring(0, token.length() - 2) + "XX";

        assertFalse(jwtUtils.isTokenValid(tampered));
    }

    @Test
    void shouldGenerateDifferentTokensForDifferentUsernames() {
        String admin = jwtUtils.generateToken("admin");
        String other = jwtUtils.generateToken("other");

        assertNotEquals(admin, other);
        assertEquals("other", jwtUtils.extractUsername(other));
    }
}
