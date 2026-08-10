package com.migration.manatal.service.job;

import com.migration.manatal.config.OwnerMappingProperties;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.service.ManatalApiClient;
import com.migration.manatal.transform.OwnerMapper;
import com.migration.manatal.transform.JobMapper;
import com.migration.manatal.transform.IndustryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManatalSourceJobServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private IndustryMapper industryMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ManatalSourceJobService service;

    private ManatalApiClient apiClient;

    @BeforeEach
    void setUp() throws Exception {
        apiClient = new ManatalApiClient(httpClient, objectMapper);
        setClientField("baseUrl", "https://api.manatal.com/open/v3/");
        setClientField("rateLimitRetrySeconds", 60);
        service = new ManatalSourceJobService(objectMapper, jobMapper, apiClient);
        setField("token", "test-source-token");
    }

    @Test
    void shouldReturnJobJsonOnOk() throws Exception {
        var ok = response(200, "{\"id\": 42, \"position_name\": \"Dev\"}");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(ok);

        String json = service.getJobById("42");

        assertTrue(json.contains("\"position_name\": \"Dev\""));
    }

    @Test
    void shouldThrowApiExceptionOnNotFound() throws Exception {
        var notFound = response(404, "not found");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(notFound);

        var ex = assertThrows(ApiException.class, () -> service.getJobById("999"));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void shouldThrowRateLimitOn429() throws Exception {
        var rateLimited = response(429, "{}");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(rateLimited);

        var ex = assertThrows(RateLimitException.class, () -> service.getJobById("42"));
        assertEquals(60, ex.getRetryAfterSeconds());
    }

    @Test
    void shouldThrowApiExceptionOnServerError() throws Exception {
        var serverError = response(500, "boom");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(serverError);

        var ex = assertThrows(ApiException.class, () -> service.getJobById("42"));
        assertEquals(500, ex.getStatus().value());
    }

    @Test
    void shouldPreviewJobMigrated() throws Exception {
        String jobJson = "{\"id\": 4081090, \"position_name\": \"Teste 2\", \"owner\": 810676}";
        var ok = response(200, jobJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(ok, ok);

        var target = new JobTarget();
        when(jobMapper.toTarget(any(), anyList())).thenReturn(target);

        var result = service.previewJobMigrated("4081090");

        assertEquals(target, result);
        verify(jobMapper).toTarget(any(), anyList());
    }

    @Test
    void shouldPreviewFullJobWithNotes() throws Exception {
        OwnerMappingProperties props = new OwnerMappingProperties();
        props.setOwnerMapping(new HashMap<>(Map.of(810676, 1234)));
        when(industryMapper.resolve("Accounting / Audit / Tax Services")).thenReturn(777);
        service = new ManatalSourceJobService(objectMapper,
                new JobMapper(new OwnerMapper(props), industryMapper), apiClient);
        setField("token", "test-source-token");

        String jobJson = """
                {
                  "id": 4081090,
                  "position_name": "Teste 2",
                  "description": "descricao",
                  "headcount": 1,
                  "creator": 810676,
                  "owner": 810676,
                  "contract_details": "full_time",
                  "is_remote": true,
                  "status": "active",
                  "custom_fields": {
                    "clientrate": "<p>teste</p>",
                    "costday": 1,
                    "rateday": 1,
                    "categoy": ["Full Stack Developer"],
                    "portugus": ["Obrigatório"],
                    "inherited": true,
                    "jobmodel": "Remote",
                    "atualstatus": "teste",
                    "contactname": "4796484",
                    "grossmargin": 1,
                    "ratehistory": "<p>1 teste</p>",
                    "businessunit": "PT -  IT",
                    "englishlevel": "Beginner (A1,A2)",
                    "startdatejob": "2026-07-16T00:00",
                    "morethanmonth": true,
                    "purchaseorder": "1",
                    "activebusiness": true,
                    "consultantname": "Teste",
                    "officelocation": ["Almada"],
                    "technicalskill": ["2D - Drawings", "1st Line"],
                    "internalization": "After 12 Months",
                    "startdatesyffer": "2026-07-13T00:00",
                    "invoicepaymentterm": "30 Days",
                    "firstjobclosedinclient": true,
                    "experiencelevelofficial": ["Middle"],
                    "replacepreviousposition": false,
                    "jobmodeldetails": "<p>teste</p>",
                    "projectnotes": "<p>Project notes</p>",
                    "lostreason": "Closed with another consultancy"
                  },
                  "industry": { "id": 384181, "name": "Accounting / Audit / Tax Services" },
                  "city": "Almada",
                  "country": "Portugal",
                  "open_at": "2026-06-26T19:01:02.533796Z",
                  "close_at": "2026-07-06T00:00:00Z"
                }
                """;

        String notesJson = """
                {"results": [
                  {"id": 1, "info": "Primeira nota", "creator": 810676, "created_at": "2026-06-27T10:00:00Z"},
                  {"id": 2, "info": "Segunda nota", "creator": 810676, "created_at": "2026-06-28T09:00:00Z"}
                ]}
                """;

        var jobResponse = response(200, jobJson);
        var notesResponse = response(200, notesJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(jobResponse, notesResponse);

        JobTarget target = service.previewJobMigrated("4081090");

        assertEquals("Teste 2", target.getPositionName());
        assertEquals(1234, target.getOwner());
        assertEquals(1, target.getHeadcount());
        assertEquals("active", target.getStatus());
        assertEquals("full_time", target.getContractDetails());
        assertEquals("Almada", target.getCity());
        assertEquals("Portugal", target.getCountry());
        assertEquals("2026-06-26", target.getOpenAt());
        assertEquals("2026-07-06", target.getCloseAt());
        assertEquals(777, target.getIndustry());
        assertEquals(Boolean.TRUE, target.getIsRemote());

        JobTarget.JobCustomFields custom = target.getCustomFields();
        assertEquals("PT - IT", custom.getBusinessUnit());
        assertEquals("teste", custom.getRate());
        assertEquals(List.of("Full Stack Developer"), custom.getCategory());
        assertEquals(List.of("Obrigatório"), custom.getPortugus());
        assertEquals("Remote", custom.getWorkplace());
        assertEquals(List.of("Almada"), custom.getOfficeLocation());
        assertEquals(List.of("Middle"), custom.getExperienceLevel());
        assertEquals("4796484", custom.getContactName());
        assertEquals(1, custom.getGrossMargin());
        assertEquals("<p>teste</p>", custom.getJobAdditionalInformation());
        assertEquals("<p>Project notes</p>", custom.getProjectNotes());
        assertEquals("Closed with another consultancy", custom.getLostReason());

        assertNotNull(target.getNotes());
        assertEquals(2, target.getNotes().size());
        assertEquals("Primeira nota", target.getNotes().get(0).getContent());
        assertEquals(810676, target.getNotes().get(0).getCreator());
        assertEquals("2026-06-27T10:00:00Z", target.getNotes().get(0).getCreatedAt());
        assertEquals("Segunda nota", target.getNotes().get(1).getContent());
    }

    @Test
    void shouldListJobsWithContactsAcrossPages() throws Exception {
        setField("pageSize", 2);

        String page1 = """
                {"results": [
                  {"id": 1, "position_name": "Dev", "custom_fields": {"contactname": "4796484"}},
                  {"id": 2, "position_name": "QA", "custom_fields": {"contactname": ""}}
                ]}
                """;
        String page2 = """
                {"results": [
                  {"id": 3, "position_name": "PM", "custom_fields": {"contactname": "5555"}}
                ]}
                """;

        var p1 = response(200, page1);
        var p2 = response(200, page2);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(p1, p2);

        var result = service.listJobsWithContacts();

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).id());
        assertEquals("4796484", result.get(0).contactName());
        assertEquals(3, result.get(1).id());
        assertEquals("PM", result.get(1).positionName());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        var uris = requestCaptor.getAllValues().stream().map(r -> r.uri().toString()).toList();
        assertTrue(uris.get(0).contains("page=1&page_size=2"));
        assertTrue(uris.get(1).contains("page=2&page_size=2"));
        assertFalse(uris.get(0).contains("offset"));
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
        var field = ManatalSourceJobService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    private void setClientField(String name, Object value) throws Exception {
        var field = ManatalApiClient.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(apiClient, value);
    }
}
