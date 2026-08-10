package com.migration.manatal.service.client;

import com.migration.manatal.config.OwnerMappingProperties;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.NonRetryableApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.client.ClientSource;
import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.service.ManatalApiClient;
import com.migration.manatal.transform.ClientMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
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

    private ManatalApiClient apiClient;

    private OwnerMappingProperties properties;

    @BeforeEach
    void setUp() throws Exception {
        apiClient = new ManatalApiClient(httpClient, objectMapper);
        setClientField("baseUrl", "https://api.manatal.com/open/v3/");
        setClientField("rateLimitRetrySeconds", 0);
        setClientField("retryLimit", 1);
        properties = new OwnerMappingProperties();
        service = new ManatalSourceClientService(objectMapper, clientMapper, apiClient, properties);
        setField("token", "test-source-token");
        setField("pageSize", 100);
    }

    @Test
    void shouldRetryRateLimitedPatchAndSucceed() throws Exception {
        setClientField("retryLimit", 2);
        var getOk = response(200, "{\"name\": \"Acme\", \"custom_fields\": {\"exported\": \"To Export\", \"foo\": \"bar\"}}");
        var rateLimited = response(429, "{}", "0");
        var ok = response(200, "{}");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(getOk, rateLimited, ok);

        service.updateCustomField("42", "exported", "Yes");

        verify(httpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
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

        var ex = assertThrows(ApiException.class, () -> service.listOrganizationsPage(1));
        assertEquals(502, ex.getStatus().value());
    }

    @Test
    void shouldReturnOnlyOrganizationsMarkedToExportAcrossPages() throws Exception {
        setField("pageSize", 2);
        var page1 = """
                {"count": 2, "next": "https://api.manatal.com/open/v3/organizations/?page=2&page_size=2", "results": [
                  {"id": "1", "name": "One", "custom_fields": {"exported": "To Export"}},
                  {"id": "2", "name": "Two", "custom_fields": {"exported": "Yes"}}
                ]}
                """;
        var page2 = """
                {"count": 2, "next": null, "results": [
                  {"id": "3", "name": "Three", "custom_fields": {"exported": "To Export"}}
                ]}
                """;

        var page1Response = response(200, page1);
        var page2Response = response(200, page2);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(page1Response, page2Response);

        var result = service.listOrganizationsWithExportFilter();

        assertEquals(2, result.size());
        assertEquals("1", result.get(0).id());
        assertEquals("One", result.get(0).name());
        assertEquals("3", result.get(1).id());
        assertEquals("Three", result.get(1).name());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        var uris = requestCaptor.getAllValues().stream().map(r -> r.uri().toString()).toList();
        assertTrue(uris.get(0).contains("page=1&page_size=2"));
        assertTrue(uris.get(1).contains("page=2&page_size=2"));
        assertFalse(uris.get(0).contains("offset"));
    }

    @Test
    void shouldStopWhenPageEmpty() throws Exception {
        var emptyResponse = response(200, """
                {"results": []}""");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(emptyResponse);

        var result = service.listOrganizationsWithExportFilter();

        assertTrue(result.isEmpty());
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void shouldThrowBadRequestOnNonNumericOrganizationId() {
        var spy = spy(service);
        doReturn("{\"name\": \"Acme\"}").when(spy).fetchOrganizationById("abc");

        var ex = assertThrows(ApiException.class, () -> spy.previewClientMigrated("abc"));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void shouldCollectNotesFromContactsAndIgnoreOrganizationNotes404() throws Exception {
        var spy = spy(service);
        doReturn("{\"id\": 42, \"name\": \"Acme\"}").when(spy).fetchOrganizationById("42");
        doReturn("{\"results\":[{\"id\":111,\"full_name\":\"Contact A\"},{\"id\":222,\"full_name\":\"Contact B\"}]}")
                .when(spy).listContactsByOrganizationPage(42, 1);
        doReturn("[{\"info\":\"note from A\",\"creator\":5,\"created_at\":\"2025-01-01T00:00:00Z\"}]")
                .when(spy).listContactNotes(111, 0);
        doReturn("[]").when(spy).listContactNotes(222, 0);
        doThrow(new NonRetryableApiException(HttpStatus.NOT_FOUND, "No Organization matches the given query."))
                .when(spy).listOrganizationNotes("42", 0);

        spy.previewClientMigrated("42");

        verify(spy).listContactNotes(111, 0);
        verify(spy).listContactNotes(222, 0);

        ArgumentCaptor<List<ClientSource.SourceNote>> notesCaptor = ArgumentCaptor.forClass(List.class);
        verify(clientMapper).toTarget(any(), any(), notesCaptor.capture());
        assertEquals(1, notesCaptor.getValue().size());
        assertEquals("note from A", notesCaptor.getValue().get(0).getContent());
    }

    @Test
    void shouldAggregateContactsAcrossPagesByOrganization() throws Exception {
        setField("pageSize", 2);
        var page1 = """
                {"count": 2, "next": "https://api.manatal.com/open/v3/contacts/?page=2&page_size=2&organization_id=42", "results": [
                  {"id": 111, "full_name": "Contact A"},
                  {"id": 222, "full_name": "Contact B"}
                ]}
                """;
        var page2 = """
                {"count": 1, "next": null, "results": [
                  {"id": 333, "full_name": "Contact C"}
                ]}
                """;

        var page1Response = response(200, page1);
        var page2Response = response(200, page2);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(page1Response, page2Response);

        var result = service.listAllContactsByOrganization(42);

        assertEquals(3, result.size());
        assertEquals("Contact A", result.get(0).getFullName());
        assertEquals("Contact B", result.get(1).getFullName());
        assertEquals("Contact C", result.get(2).getFullName());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        var uris = requestCaptor.getAllValues().stream().map(r -> r.uri().toString()).toList();
        assertTrue(uris.get(0).contains("page=1&page_size=2"));
        assertTrue(uris.get(1).contains("page=2&page_size=2"));
        assertFalse(uris.get(0).contains("offset"));
    }

    @Test
    void shouldResolveCreatorNamesFromUsersOnPreview() throws Exception {
        var spy = spy(service);
        doReturn("{\"id\": 42, \"name\": \"Acme\"}").when(spy).fetchOrganizationById("42");
        doReturn("{\"results\":[{\"id\":111,\"full_name\":\"Contact A\"}]}")
                .when(spy).listContactsByOrganizationPage(42, 1);
        doReturn("[{\"info\":\"note from A\",\"creator\":5,\"created_at\":\"2025-01-01T00:00:00Z\"}]")
                .when(spy).listContactNotes(111, 0);
        doReturn("[]").when(spy).listOrganizationNotes("42", 0);
        doReturn(Map.of(5, "Maria Silva")).when(spy).listUsersBestEffort();

        spy.previewClientMigrated("42");

        ArgumentCaptor<List<ClientSource.SourceNote>> notesCaptor = ArgumentCaptor.forClass(List.class);
        verify(clientMapper).toTarget(any(), any(), notesCaptor.capture());
        assertEquals("Maria Silva", notesCaptor.getValue().get(0).getCreatorName());
        assertEquals(5, notesCaptor.getValue().get(0).getCreator());
    }

    @Test
    void shouldFallbackToConfiguredCreatorNameWhenUserDeleted() throws Exception {
        properties.getCreatorNameMapping().put(234956, "Ex-Colega");
        var spy = spy(service);
        doReturn("{\"id\": 42, \"name\": \"Acme\"}").when(spy).fetchOrganizationById("42");
        doReturn("{\"results\":[{\"id\":111,\"full_name\":\"Contact A\"}]}")
                .when(spy).listContactsByOrganizationPage(42, 1);
        doReturn("[{\"info\":\"nota antiga\",\"creator\":234956,\"created_at\":\"2024-01-10T00:00:00Z\"}]")
                .when(spy).listContactNotes(111, 0);
        doReturn("[]").when(spy).listOrganizationNotes("42", 0);
        doReturn(Map.of()).when(spy).listUsersBestEffort();

        spy.previewClientMigrated("42");

        ArgumentCaptor<List<ClientSource.SourceNote>> notesCaptor = ArgumentCaptor.forClass(List.class);
        verify(clientMapper).toTarget(any(), any(), notesCaptor.capture());
        assertEquals("Ex-Colega", notesCaptor.getValue().get(0).getCreatorName());
        assertEquals(234956, notesCaptor.getValue().get(0).getCreator());
    }

    @Test
    void shouldHandleOrganizationWithNoContacts() throws Exception {
        var spy = spy(service);
        doReturn("{\"id\": 42, \"name\": \"Acme\"}").when(spy).fetchOrganizationById("42");
        doReturn("{\"results\":[]}").when(spy).listContactsByOrganizationPage(42, 1);
        doReturn("[]").when(spy).listOrganizationNotes("42", 0);

        spy.previewClientMigrated("42");

        ArgumentCaptor<List<ClientSource.SourceNote>> notesCaptor = ArgumentCaptor.forClass(List.class);
        verify(clientMapper).toTarget(any(), any(), notesCaptor.capture());
        assertTrue(notesCaptor.getValue().isEmpty());
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

    private void setClientField(String name, Object value) throws Exception {
        var field = ManatalApiClient.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(apiClient, value);
    }
}
