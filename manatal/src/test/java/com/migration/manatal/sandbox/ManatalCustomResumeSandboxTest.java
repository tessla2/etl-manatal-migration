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
class ManatalCustomResumeSandboxTest {

    private static final String BASE_URL = System.getenv().getOrDefault("MANATAL_APP_BASE_URL", "https://app.manatal.com/api/v1.0/");
    private static final String TOKEN = System.getenv().getOrDefault("MANATAL_TOKEN", "");
    private static final String CANDIDATE_ID = System.getenv().getOrDefault("MANATAL_CANDIDATE_ID", "");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ManatalApiClient apiClient;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(!TOKEN.isBlank(), "Skipped: set MANATAL_TOKEN (source API key)");
        assumeTrue(!CANDIDATE_ID.isBlank(), "Skipped: set MANATAL_CANDIDATE_ID");

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
    void shouldInspectCustomResumeEndpoints() throws Exception {
        String id = CANDIDATE_ID.trim();

        String listJson = apiClient.get(
                apiClient.endpoint("/candidates/" + id + "/custom-resumes/"), TOKEN, "listing custom resumes for candidate " + id);
        log.info("=== GET /candidates/{}/custom-resumes/ ===", id);
        log.info(listJson);

        JsonNode list = objectMapper.readTree(listJson);
        JsonNode array = list.isArray() ? list : list.path("results");
        if (array.isArray() && !array.isEmpty()) {
            long resumeId = array.get(0).path("id").asLong(0);
            if (resumeId == 0) {
                resumeId = array.get(0).path("resume_id").asLong(0);
            }
            if (resumeId > 0) {
                String detailJson = apiClient.get(
                        apiClient.endpoint("/candidates/" + id + "/custom-resumes/" + resumeId + "/"),
                        TOKEN, "fetching custom resume " + resumeId);
                log.info("=== GET /candidates/{}/custom-resumes/{}/ ===", id, resumeId);
                log.info(detailJson);
            }
        }
    }
}
