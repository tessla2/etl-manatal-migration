package com.migration.manatal.service.job;

import com.migration.manatal.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManatalTargetJobServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ManatalTargetJobService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ManatalTargetJobService(httpClient, objectMapper);
        var baseUrlField = ManatalTargetJobService.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(service, "https://api.manatal.com/open/v3/");

        var tokenField = ManatalTargetJobService.class.getDeclaredField("targetToken");
        tokenField.setAccessible(true);
        tokenField.set(service, "test-target-token");
    }

    @Test
    void shouldPostNoteAndReturnResponse() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\": 5, \"info\": \"nota escrita a mao\"}");

        var result = service.createJobNote(42, "nota escrita a mao");

        assertEquals("{\"id\": 5, \"info\": \"nota escrita a mao\"}", result);
    }

    @Test
    void shouldThrowApiExceptionOnHttpError() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("{\"error\": \"bad request\"}");

        var ex = assertThrows(ApiException.class, () -> service.createJobNote(42, "nota"));

        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void shouldThrowApiExceptionOnException() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("connection refused"));

        var ex = assertThrows(ApiException.class, () -> service.createJobNote(42, "nota"));

        assertEquals(502, ex.getStatus().value());
    }
}
