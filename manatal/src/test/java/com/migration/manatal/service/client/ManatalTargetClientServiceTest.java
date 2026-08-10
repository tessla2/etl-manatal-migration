package com.migration.manatal.service.client;

import com.migration.manatal.exception.ApiException;
import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.service.ManatalApiClient;
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

    private ManatalApiClient apiClient;

    private ManatalTargetClientService service;

    @BeforeEach
    void setUp() throws Exception {
        apiClient = new ManatalApiClient(httpClient, objectMapper);
        var baseUrlField = ManatalApiClient.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(apiClient, "https://api.manatal.com/open/v3/");

        service = new ManatalTargetClientService(apiClient);

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

    @Test
    void shouldCreateOrganizationNote() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\": 99}");

        var result = service.createOrganizationNote(42, "hello note");

        assertEquals("{\"id\": 99}", result);
    }

    @Test
    void shouldCreateContactNote() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\": 98}");

        var result = service.createContactNote(7L, "hello note");

        assertEquals("{\"id\": 98}", result);
    }

    @Test
    void shouldCreateContact() throws Exception {
        var contact = new ClientTarget.ContactTarget();
        contact.setFullName("John Doe");
        contact.setOrganization(42L);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\": 7}");

        var result = service.createContact(contact);

        assertEquals("{\"id\": 7}", result);
    }
}
