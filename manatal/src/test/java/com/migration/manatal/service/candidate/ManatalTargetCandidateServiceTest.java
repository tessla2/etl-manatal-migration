package com.migration.manatal.service.candidate;

import com.migration.manatal.exception.ApiException;
import com.migration.manatal.model.candidate.CandidateTarget;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManatalTargetCandidateServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ManatalApiClient apiClient;

    private ManatalTargetCandidateService service;

    @BeforeEach
    void setUp() throws Exception {
        apiClient = new ManatalApiClient(httpClient, objectMapper);
        var baseUrlField = ManatalApiClient.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(apiClient, "https://api.manatal.com/open/v3/");

        service = new ManatalTargetCandidateService(apiClient);

        var tokenField = ManatalTargetCandidateService.class.getDeclaredField("targetToken");
        tokenField.setAccessible(true);
        tokenField.set(service, "test-target-token");
    }

    @Test
    void shouldMigrateCandidateAndReturnResponse() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\": 10}");

        CandidateTarget target = new CandidateTarget();
        target.setFullName("Candidato Teste");

        var result = service.migrateCandidate(target);

        assertEquals("{\"id\": 10}", result);
    }

    @Test
    void shouldCreateNoteAndReturnResponse() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\": 3, \"info\": \"nota\"}");

        var result = service.createCandidateNote(42, "nota");

        assertEquals("{\"id\": 3, \"info\": \"nota\"}", result);
    }

    @Test
    void shouldCreateNationalityAndReturnResponse() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\": 7, \"country\": \"Portugal\"}");

        var result = service.createCandidateNationality(42, "Portugal");

        assertEquals("{\"id\": 7, \"country\": \"Portugal\"}", result);
    }

    @Test
    void shouldCreateSocialMediaAndReturnResponse() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\": 5, \"social_media\": \"linkedin\", \"social_media_url\": \"https://linkedin.com/in/user\"}");

        var result = service.createCandidateSocialMedia(42, "linkedin", "https://linkedin.com/in/user");

        assertEquals("{\"id\": 5, \"social_media\": \"linkedin\", \"social_media_url\": \"https://linkedin.com/in/user\"}", result);
    }

    @Test
    void shouldAddSkillsAndReturnResponse() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"skills\": [{\"skill_name\": \"Java\"}]}");

        CandidateTarget.TargetSkill skill = new CandidateTarget.TargetSkill();
        skill.setSkillName("Java");
        skill.setScore(7);

        var result = service.addCandidateSkills(42, List.of(skill));

        assertEquals("{\"skills\": [{\"skill_name\": \"Java\"}]}", result);
    }

    @Test
    void shouldCreateMatchAndReturnResponse() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\": 9, \"job\": 4081090, \"candidate\": 42}");

        var result = service.createCandidateMatch(42, 4081090);

        assertEquals("{\"id\": 9, \"job\": 4081090, \"candidate\": 42}", result);
    }

    @Test
    void shouldUpdateMatchStageAndReturnResponse() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"id\": 9, \"job_pipeline_stage\": {\"id\": 306977}}");

        var result = service.updateMatchStage(9, 306977);

        assertEquals("{\"id\": 9, \"job_pipeline_stage\": {\"id\": 306977}}", result);
    }

    @Test
    void shouldCreateMatchNoteAndReturnResponse() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\": 3, \"info\": \"Dropado\"}");

        var result = service.createMatchNote(9, "Dropado");

        assertEquals("{\"id\": 3, \"info\": \"Dropado\"}", result);
    }

    @Test
    void shouldDropMatchAndReturnResponse() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"id\": 9, \"is_active\": false, \"dropped_at\": \"2026-08-05T10:00:00\"}");

        var result = service.dropMatch(9, "2026-08-05T10:00:00");

        assertEquals("{\"id\": 9, \"is_active\": false, \"dropped_at\": \"2026-08-05T10:00:00\"}", result);
    }

    @Test
    void shouldListJobPipelines() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"results\": []}");

        var result = service.getJobPipelines();

        assertEquals("{\"results\": []}", result);
    }

    @Test
    void shouldListCandidateMatches() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"results\": []}");

        var result = service.getCandidateMatches(42);

        assertEquals("{\"results\": []}", result);
    }

    @Test
    void shouldFetchTargetJob() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"id\": 4081090, \"job_pipeline\": 29324}");

        var result = service.getTargetJob(4081090);

        assertEquals("{\"id\": 4081090, \"job_pipeline\": 29324}", result);
    }

    @Test
    void shouldThrowApiExceptionOnHttpError() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(400);
        when(httpResponse.body()).thenReturn("{\"error\": \"bad request\"}");

        var ex = assertThrows(ApiException.class, () -> service.createCandidateMatch(42, 4081090));

        assertEquals(400, ex.getStatus().value());
    }
}
