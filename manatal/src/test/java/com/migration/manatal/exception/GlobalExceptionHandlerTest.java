package com.migration.manatal.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldMapApiExceptionToItsStatus() {
        var response = handler.handleApiException(ApiException.notFound("missing org"));

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
        assertEquals("missing org", response.getBody().message());
    }

    @Test
    void shouldMapRateLimitTo429() {
        var response = handler.handleRateLimit(new RateLimitException(30));

        assertEquals(429, response.getStatusCode().value());
        assertEquals(429, response.getBody().status());
    }

    @Test
    void shouldMapGenericExceptionTo500() {
        var response = handler.handleGeneric(new IllegalStateException("boom"));

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Internal server error", response.getBody().message());
    }
}
