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
class ManatalCandidateDumpSandboxTest {

    private static final String BASE_URL = System.getenv().getOrDefault("MANATAL_BASE_URL", "https://api.manatal.com/open/v3/");
    private static final String TOKEN = System.getenv().getOrDefault("MANATAL_TOKEN",
            System.getenv().getOrDefault("MANATAL_TARGET_TOKEN", ""));
    private static final String CANDIDATE_ID = System.getenv().getOrDefault("MANATAL_CANDIDATE_ID", "");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ManatalApiClient apiClient;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(!TOKEN.isBlank(), "Skipped: set MANATAL_TOKEN (source) to inspect the candidate payload");
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
    void shouldDumpFullCandidatePayloadAndSubResources() throws Exception {
        long id = Long.parseLong(CANDIDATE_ID.trim());

        log.info("=== GET /candidates/{}/ (FULL PAYLOAD) ===", id);
        String detailJson = apiClient.get(
                apiClient.endpoint("/candidates/" + id + "/"), TOKEN, "fetching candidate " + id);
        JsonNode detail = objectMapper.readTree(detailJson);
        log.info("== top-level keys ==");
        for (String name : detail.propertyNames()) {
            log.info("  {}", name);
        }
        log.info("== full JSON ==");
        log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(detail));

        dumpSubResource(id, "/nationalities/", "nationalities");
        dumpSubResource(id, "/skills/", "skills");
        dumpSubResource(id, "/tags/", "tags");
        dumpSubResource(id, "/notes/", "notes");
        dumpSubResource(id, "/resume/", "resume");
        dumpSubResource(id, "/attachments/", "attachments");
        dumpSubResource(id, "/educations/", "educations");
        dumpSubResource(id, "/experiences/", "experiences");
        dumpSubResource(id, "/matches/", "matches");
        dumpSubResource(id, "/activities/", "activities");
        dumpSubResource(id, "/social-media/", "social-media");
    }

    private void dumpSubResource(long id, String suffix, String label) {
        try {
            String json = apiClient.get(
                    apiClient.endpoint("/candidates/" + id + suffix), TOKEN, "fetching candidate " + label);
            log.info("=== GET /candidates/{}{} ({}) ===", id, suffix, label);
            log.info(objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(objectMapper.readTree(json)));
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.info("=== GET /candidates/{}{} ({}) -> ERROR: {} ===", id, suffix, label, msg);
        }
    }
}
