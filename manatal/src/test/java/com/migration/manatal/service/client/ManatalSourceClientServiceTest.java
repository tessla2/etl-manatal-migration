package com.migration.manatal.service.client;

import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.transform.ClientMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManatalSourceClientServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private ClientMapper clientMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ManatalSourceClientService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new ManatalSourceClientService(httpClient, objectMapper, clientMapper);
        setField("baseUrl", "https://api.manatal.com/open/v3/");
        setField("token", "test-source-token");
        setField("rateLimitRetrySeconds", 0);
        setField("retryLimit", 1);
    }

    @Test
    void shouldRetryRateLimitedPatchAndSucceed() throws Exception {
        setField("retryLimit", 2);
        var rateLimited = response(429, "{}", "0");
        var ok = response(200, "{}");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(rateLimited, ok);

        service.updateCustomField("42", "exported", "Yes");

        verify(httpClient, times(2)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void shouldRethrowRateLimitAfterRetriesExhausted() throws Exception {
        var rateLimited = response(429, "{}", "0");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(rateLimited);

        var ex = assertThrows(RateLimitException.class,
                () -> service.updateCustomField("42", "exported", "Yes"));
        assertEquals(0, ex.getRetryAfterSeconds());
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void shouldMapUnknownHttpStatusToBadGatewayOnGet() throws Exception {
        var unknown = response(520, "unknown status");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(unknown);

        var ex = assertThrows(ApiException.class, () -> service.listOrganizationsWithExportFilter(0));
        assertEquals(502, ex.getStatus().value());
    }

    @Test
    void shouldThrowBadRequestOnNonNumericOrganizationId() {
        var spy = spy(service);
        doReturn("{\"name\": \"Acme\"}").when(spy).fetchOrganizationById("abc");

        var ex = assertThrows(ApiException.class, () -> spy.previewClientMigrated("abc"));
        assertEquals(400, ex.getStatus().value());
    }

    private HttpResponse<String> response(int code, String body) {
        return response(code, body, null);
    }

    private HttpResponse<String> response(int code, String body, String retryAfter) {
        HttpResponse<String> resp = mock(HttpResponse.class);
        lenient().when(resp.statusCode()).thenReturn(code);
        lenient().when(resp.body()).thenReturn(body);
        HttpHeaders headers = mock(HttpHeaders.class);
        lenient().when(headers.firstValue("Retry-After")).thenReturn(Optional.ofNullable(retryAfter));
        lenient().when(resp.headers()).thenReturn(headers);
        return resp;
    }

    private void setField(String name, Object value) throws Exception {
        var field = ManatalSourceClientService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }
}
