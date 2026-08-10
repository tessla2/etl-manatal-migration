package com.migration.manatal.service.candidate;

import com.migration.manatal.exception.NonRetryableApiException;
import com.migration.manatal.model.candidate.CandidateSource;
import com.migration.manatal.model.candidate.CandidateTarget;
import com.migration.manatal.service.ManatalApiClient;
import com.migration.manatal.transform.CandidateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManatalSourceCandidateServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private CandidateMapper candidateMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ManatalApiClient apiClient;

    private ManatalSourceCandidateService service;

    @BeforeEach
    void setUp() throws Exception {
        apiClient = new ManatalApiClient(httpClient, objectMapper);
        var baseUrlField = ManatalApiClient.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(apiClient, "https://api.manatal.com/open/v3/");

        service = new ManatalSourceCandidateService(objectMapper, candidateMapper, apiClient);

        var tokenField = ManatalSourceCandidateService.class.getDeclaredField("token");
        tokenField.setAccessible(true);
        tokenField.set(service, "test-source-token");

        var pageSizeField = ManatalSourceCandidateService.class.getDeclaredField("pageSize");
        pageSizeField.setAccessible(true);
        pageSizeField.set(service, 100);
    }

    private void stubGet(Map<String, String> responses) throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    HttpRequest request = invocation.getArgument(0);
                    String uri = request.uri().toString();
                    for (var entry : responses.entrySet()) {
                        if (uri.contains(entry.getKey())) {
                            return jsonResponse(entry.getValue());
                        }
                    }
                    throw new AssertionError("Unexpected URI: " + uri);
                });
    }

    private HttpResponse<String> jsonResponse(String body) {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return 200;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(Map.of(), (name, value) -> true);
            }

            @Override
            public String body() {
                return body;
            }

            @Override
            public Optional<javax.net.ssl.SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
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
        var field = ManatalSourceCandidateService.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(service, value);
    }

    @Test
    void shouldListCandidatesWithPagination() throws Exception {
        stubGet(Map.of("/candidates/?limit=100&offset=0", "{\"results\": []}"));

        var result = service.listCandidates(0);

        assertEquals("{\"results\": []}", result);
    }

    @Test
    void shouldReturnOnlyCandidatesWithExportTagAcrossPages() throws Exception {
        setField("pageSize", 2);
        var page1 = """
                {"count": 2, "next": "https://api.manatal.com/open/v3/candidates/?page=2&page_size=2", "results": [
                  {"id": "201", "full_name": "Ana Silva", "candidate_tags": [{"tag_id": 1, "tag_name": "To Export"}]},
                  {"id": "202", "full_name": "Bruno Souza", "candidate_tags": [{"tag_id": 2, "tag_name": "Outra Tag"}]}
                ]}
                """;
        var page2 = """
                {"count": 1, "next": null, "results": [
                  {"id": "203", "full_name": "Carla Lima", "candidate_tags": [{"tag": "To Export"}]}
                ]}
                """;

        var page1Response = response(200, page1);
        var page2Response = response(200, page2);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(page1Response, page2Response);

        var result = service.listCandidatesWithExportFilter();

        assertEquals(2, result.size());
        assertEquals("201", result.get(0).id());
        assertEquals("Ana Silva", result.get(0).fullName());
        assertEquals("203", result.get(1).id());
        assertEquals("Carla Lima", result.get(1).fullName());

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        var uris = requestCaptor.getAllValues().stream().map(r -> r.uri().toString()).toList();
        assertTrue(uris.get(0).contains("page=1&page_size=2"));
        assertTrue(uris.get(0).contains("candidate_tags=To%20Export"));
        assertTrue(uris.get(1).contains("page=2&page_size=2"));
        assertFalse(uris.get(0).contains("offset"));
    }

    @Test
    void shouldStopWhenCandidatePageEmpty() throws Exception {
        var emptyResponse = response(200, """
                {"results": []}""");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(emptyResponse);

        var result = service.listCandidatesWithExportFilter();

        assertTrue(result.isEmpty());
        verify(httpClient, times(1)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void shouldUpdateCustomFieldMergingExistingFields() throws Exception {
        var getResp = response(200, "{\"id\": \"123\", \"full_name\": \"Ana Silva\", "
                + "\"custom_fields\": {\"ratehistory\": \"<p>x</p>\", \"exported\": \"To Export\"}}");
        var patchResp = response(200, "{\"id\": \"123\"}");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> {
                    HttpRequest request = invocation.getArgument(0);
                    return request.method().equals("GET") ? getResp : patchResp;
                });

        service.updateCustomField("123", "exported", "Yes");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        var requests = requestCaptor.getAllValues();
        assertEquals("GET", requests.get(0).method());
        assertEquals("PATCH", requests.get(1).method());
        assertTrue(requests.get(1).uri().toString().contains("/candidates/123/"));

        String patchBody = readBody(requests.get(1));
        assertTrue(patchBody.contains("\"ratehistory\":\"<p>x</p>\"") || patchBody.contains("\"ratehistory\": \"<p>x</p>\""));
        assertTrue(patchBody.contains("\"exported\":\"Yes\"") || patchBody.contains("\"exported\": \"Yes\""));
    }

    @Test
    void shouldAddCandidateTagToSource() throws Exception {
        var postResp = response(201, "{\"id\": 55, \"tag\": \"Exported\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(postResp);

        service.addCandidateTag("123", "Exported");

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(1)).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest request = requestCaptor.getValue();
        assertEquals("POST", request.method());
        assertTrue(request.uri().toString().contains("/candidates/123/tags/"));
        String body = readBody(request);
        assertTrue(body.contains("\"tag\":\"Exported\"") || body.contains("\"tag\": \"Exported\""));
    }

    @Test
    void shouldFindCandidateTagIdByName() throws Exception {
        stubGet(Map.of("/candidates/123/",
                "{\"id\": 123, \"full_name\": \"Ana Silva\", \"candidate_tags\": ["
                        + "{\"id\": 777, \"tag_id\": 9, \"tag_name\": \"To Export\"},"
                        + "{\"id\": 888, \"tag_id\": 10, \"tag_name\": \"Outra\"}]}"));

        Long tagId = service.getCandidateTagId("123", "To Export");

        assertEquals(777L, tagId);
    }

    @Test
    void shouldReturnNullWhenCandidateTagNotFound() throws Exception {
        stubGet(Map.of("/candidates/123/",
                "{\"id\": 123, \"full_name\": \"Ana Silva\", \"candidate_tags\": ["
                        + "{\"id\": 888, \"tag_id\": 10, \"tag_name\": \"Outra\"}]}"));

        Long tagId = service.getCandidateTagId("123", "To Export");

        assertNull(tagId);
    }

    @Test
    void shouldRemoveCandidateTagFromSource() throws Exception {
        var deleteResp = response(204, "");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(deleteResp);

        service.removeCandidateTag("123", 777L);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(1)).send(requestCaptor.capture(), any(HttpResponse.BodyHandler.class));
        HttpRequest request = requestCaptor.getValue();
        assertEquals("DELETE", request.method());
        assertTrue(request.uri().toString().contains("/candidates/123/tags/777/"));
    }

    private String readBody(HttpRequest request) throws Exception {
        var publisher = request.bodyPublisher().orElseThrow();
        var bos = new java.io.ByteArrayOutputStream();
        var done = new java.util.concurrent.CompletableFuture<byte[]>();
        publisher.subscribe(new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
            @Override
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(java.nio.ByteBuffer buffer) {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                bos.write(bytes, 0, bytes.length);
            }

            @Override
            public void onError(Throwable throwable) {
                done.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                done.complete(bos.toByteArray());
            }
        });
        return new String(done.get(), java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void shouldPreviewCandidateMigrated() throws Exception {
        stubGet(Map.of(
                "/candidates/123/notes/",
                "[{\"id\": 1, \"info\": \"nota\", \"created_at\": \"2026-01-01T00:00:00Z\"}]",
                "/candidates/123/nationalities/",
                "{\"results\": [{\"id\": 5, \"country\": \"Portugal\"}]}",
                "/candidates/123/",
                "{\"id\": 123, \"full_name\": \"Ana Silva\", \"skills\": [{\"skill_name\": \"Java\", \"score\": 7}]}"));

        CandidateTarget target = new CandidateTarget();
        target.setFullName("Ana Silva");
        when(candidateMapper.toTarget(any(CandidateSource.class), any(), any(), any())).thenReturn(target);

        CandidateTarget result = service.previewCandidateMigrated("123");

        assertEquals("Ana Silva", result.getFullName());
        verify(candidateMapper).toTarget(any(CandidateSource.class), any(), any(), any());
    }

    @Test
    void shouldReturnResumeParsed() throws Exception {
        stubGet(Map.of("/candidates/123/resume/",
                "{\"id\": 1, \"resume_file\": \"https://s3/cv.pdf\", \"created_at\": \"2026-01-01T00:00:00Z\"}"));

        CandidateSource.Resume resume = service.getCandidateResume("123");

        assertNotNull(resume);
        assertEquals("https://s3/cv.pdf", resume.getResumeFile());
    }

    @Test
    void shouldReturnNullResumeOnNotFound() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> jsonResponse404());

        CandidateSource.Resume resume = service.getCandidateResume("123");

        assertNull(resume);
    }

    private HttpResponse<String> jsonResponse404() {
        return new HttpResponse<>() {
            @Override
            public int statusCode() {
                return 404;
            }

            @Override
            public HttpRequest request() {
                return null;
            }

            @Override
            public Optional<HttpResponse<String>> previousResponse() {
                return Optional.empty();
            }

            @Override
            public HttpHeaders headers() {
                return HttpHeaders.of(Map.of(), (name, value) -> true);
            }

            @Override
            public String body() {
                return "not found";
            }

            @Override
            public Optional<javax.net.ssl.SSLSession> sslSession() {
                return Optional.empty();
            }

            @Override
            public URI uri() {
                return null;
            }

            @Override
            public HttpClient.Version version() {
                return HttpClient.Version.HTTP_1_1;
            }
        };
    }

    @Test
    void shouldPageThroughCandidateMatches() throws Exception {
        setField("pageSize", 2);

        stubGet(Map.of(
                "page=1&page_size=2", "{\"results\": [{\"id\": 1, \"job\": 11}, {\"id\": 2, \"job\": 12}]}",
                "page=2&page_size=2", "{\"results\": []}"));

        List<CandidateSource.CandidateMatch> matches = service.getCandidateMatches("123");

        assertEquals(2, matches.size());
        assertEquals(11L, matches.get(0).getJob());
        assertEquals(12L, matches.get(1).getJob());
    }

    @Test
    void shouldListCandidateAttachments() throws Exception {
        stubGet(Map.of("/candidates/123/attachments/",
                "{\"results\": [{\"id\": 1, \"name\": \"doc\", \"file\": \"https://s3/doc.pdf\"}]}"));

        List<CandidateSource.Attachment> attachments = service.getCandidateAttachments("123");

        assertEquals(1, attachments.size());
        assertEquals("doc", attachments.get(0).getName());
        assertEquals("https://s3/doc.pdf", attachments.get(0).getFile());
    }

    @Test
    void shouldListCandidateSocialMedia() throws Exception {
        stubGet(Map.of("/candidates/123/social-media/",
                "[{\"id\": 1, \"social_media\": \"linkedin\", \"social_media_url\": \"https://linkedin.com/in/user\"}]"));

        List<CandidateSource.SocialMedia> socialMedia = service.getCandidateSocialMedia("123");

        assertEquals(1, socialMedia.size());
        assertEquals("linkedin", socialMedia.get(0).getSocialMedia());
        assertEquals("https://linkedin.com/in/user", socialMedia.get(0).getSocialMediaUrl());
    }

    @Test
    void shouldListCandidateActivities() throws Exception {
        stubGet(Map.of("/candidates/123/activities/",
                "[{\"id\": 1, \"name\": \"Entrevista\", \"description\": \"Candidato foi bem\", "
                        + "\"due_date\": \"2026-08-05T10:00:00\", \"creator\": 5}]"));

        List<CandidateSource.Activity> activities = service.getCandidateActivities("123");

        assertEquals(1, activities.size());
        assertEquals("Entrevista", activities.get(0).getName());
        assertEquals("Candidato foi bem", activities.get(0).getDescription());
        assertEquals("2026-08-05T10:00:00", activities.get(0).getDueDate());
        assertEquals(5, activities.get(0).getCreator());
    }

    @Test
    void shouldListUserDisplayNamesBestEffort() throws Exception {
        stubGet(Map.of(
                "/users/?page=1&page_size=100",
                "{\"results\": [{\"id\": 5, \"display_name\": \"Ana Silva\"}]}",
                "/users/?page=2&page_size=100",
                "{\"results\": []}"));

        Map<Integer, String> users = service.listUsersBestEffort();

        assertEquals(Map.of(5, "Ana Silva"), users);
    }

    @Test
    void shouldThrowNonRetryableWhenCandidateNotFound() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(invocation -> jsonResponse404());

        var ex = assertThrows(NonRetryableApiException.class, () -> service.fetchCandidateById("999"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
