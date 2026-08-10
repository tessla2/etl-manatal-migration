package com.migration.manatal.sandbox;

import com.migration.manatal.service.ManatalApiClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Slf4j
class ManatalCandidateResumeSandboxTest {

    private static final String BASE_URL = System.getenv().getOrDefault("MANATAL_BASE_URL", "https://api.manatal.com/open/v3/");
    private static final String TOKEN = System.getenv().getOrDefault("MANATAL_TOKEN",
            System.getenv().getOrDefault("MANATAL_TARGET_TOKEN", ""));
    private static final String CANDIDATE_ID = System.getenv().getOrDefault("MANATAL_CANDIDATE_ID", "");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ManatalApiClient apiClient;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(!TOKEN.isBlank(), "Skipped: set MANATAL_TOKEN (source) or MANATAL_TARGET_TOKEN to inspect resume payloads");
        assumeTrue(!CANDIDATE_ID.isBlank(), "Skipped: set MANATAL_CANDIDATE_ID to the candidate id to inspect");

        apiClient = new ManatalApiClient(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(), objectMapper);

        var baseUrlField = ManatalApiClient.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(apiClient, BASE_URL);

        var retrySecondsField = ManatalApiClient.class.getDeclaredField("rateLimitRetrySeconds");
        retrySecondsField.setAccessible(true);
        retrySecondsField.set(apiClient, 1);

        var retryLimitField = ManatalApiClient.class.getDeclaredField("retryLimit");
        retryLimitField.setAccessible(true);
        retryLimitField.set(apiClient, 3);
    }

    @Test
    void shouldInspectCandidateResumePayloads() throws Exception {
        long id = Long.parseLong(CANDIDATE_ID.trim());

        String detailJson = apiClient.get(
                apiClient.endpoint("/candidates/" + id + "/"), TOKEN, "fetching candidate " + id);
        JsonNode detail = objectMapper.readTree(detailJson);
        log.info("=== CANDIDATE {} ===", id);
        log.info("full_name = {}", detail.path("full_name").asText());
        log.info("candidate.resume = {}", detail.path("resume").asText());
        log.info("candidate.custom_fields = {}", detail.path("custom_fields"));
        if (detail.has("custom_resume")) {
            log.info("candidate.custom_resume = {}", detail.path("custom_resume"));
        }

        String resumesJson = apiClient.get(
                apiClient.endpoint("/candidates/" + id + "/resume/"), TOKEN, "listing resumes for candidate " + id);
        JsonNode resumes = objectMapper.readTree(resumesJson);
        log.info("=== GET /candidates/{}/resume/ (OPEN API) -> {} ===", id,
                resumes.isArray() ? resumes.size() + " item(s)" : "single object (original resume only)");
        log.info(resumesJson);

        String customBase = "https://app.manatal.com/api/v1.0";
        String resumeId = System.getenv().getOrDefault("MANATAL_CUSTOM_RESUME_ID", "").trim();

        if (!resumeId.isBlank()) {
            log.info("=== GET {}/candidates/{}/custom-resumes/{}/ (INTERNAL API detail) ===", customBase, id, resumeId);
            log.info(probe(customBase + "/candidates/" + id + "/custom-resumes/" + resumeId + "/", "custom resume detail " + resumeId));
        }

        String[] listCandidates = {
                customBase + "/candidates/" + id + "/custom-resumes/",
                customBase + "/candidates/" + id + "/resumes/",
                customBase + "/custom-resumes/?candidate=" + id,
        };
        for (String path : listCandidates) {
            log.info("=== PROBE {} ===", path);
            log.info(probe(path, "probing custom resume list path"));
        }
    }

    private String probe(String url, String context) {
        try {
            return apiClient.get(url, TOKEN, context);
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            return "ERROR: " + msg;
        }
    }
}
