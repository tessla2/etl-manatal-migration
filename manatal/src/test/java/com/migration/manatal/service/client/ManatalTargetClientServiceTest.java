package com.migration.manatal.service.client;

import com.migration.manatal.exception.ApiException;
import com.migration.manatal.model.client.ClientTarget;
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
class ManatalTargetClientServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ManatalTargetClientService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ManatalTargetClientService(httpClient, objectMapper);
        var baseUrlField = ManatalTargetClientService.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(service, "https://api.manatal.com/open/v3/");

        var tokenField = ManatalTargetClientService.class.getDeclaredField("targetToken");
        tokenField.setAccessible(true);
        tokenField.set(service, "test-target-token");
    }

    @Test
    void shouldPostOrganizationAndReturnResponse() throws Exception {
        var target = new ClientTarget();
        target.setClientName("Acme Corp");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\": 42, \"name\": \"Acme Corp\"}");

        var result = service.migrateOrganization(target);

        assertEquals("{\"id\": 42, \"name\": \"Acme Corp\"}", result);
    }

    @Test
    void shouldAcceptHttp200AsSuccess() throws Exception {
        var target = new ClientTarget();
        target.setClientName("Acme Corp");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"id\": 42}");

        var result = service.migrateOrganization(target);

        assertEquals("{\"id\": 42}", result);
    }

    @Test
    void shouldThrowApiExceptionOnHttpError() throws Exception {
        var target = new ClientTarget();
        target.setClientName("Acme Corp");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("{\"error\": \"bad request\"}");

        var ex = assertThrows(ApiException.class, () -> service.migrateOrganization(target));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void shouldThrowApiExceptionOnException() throws Exception {
        var target = new ClientTarget();
        target.setClientName("Acme Corp");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("connection refused"));

        var ex = assertThrows(ApiException.class, () -> service.migrateOrganization(target));
        assertEquals(502, ex.getStatus().value());
    }
}
