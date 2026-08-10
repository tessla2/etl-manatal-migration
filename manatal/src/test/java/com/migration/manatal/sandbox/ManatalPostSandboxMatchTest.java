package com.migration.manatal.sandbox;

import com.migration.manatal.config.OwnerMappingProperties;
import com.migration.manatal.model.candidate.CandidateSource;
import com.migration.manatal.service.ManatalApiClient;
import com.migration.manatal.service.candidate.ManatalTargetCandidateService;
import com.migration.manatal.transform.CandidateMapper;
import com.migration.manatal.transform.OwnerMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@EnabledIfEnvironmentVariable(named = "MANATAL_TARGET_TOKEN", matches = ".+")
class ManatalPostSandboxMatchTest {

    private static final String BASE_URL = System.getenv().getOrDefault("MANATAL_BASE_URL", "https://api.manatal.com/open/v3/");
    private static final int JOB_ID = Integer.parseInt(System.getenv().getOrDefault("MANATAL_JOB_ID", "4081090"));

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CandidateMapper candidateMapper = new CandidateMapper(new OwnerMapper(new OwnerMappingProperties()));
    private ManatalTargetCandidateService targetService;

    @BeforeEach
    void setUp() throws Exception {
        ManatalApiClient apiClient = new ManatalApiClient(
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

        targetService = new ManatalTargetCandidateService(apiClient);

        var tokenField = ManatalTargetCandidateService.class.getDeclaredField("targetToken");
        tokenField.setAccessible(true);
        tokenField.set(targetService, System.getenv("MANATAL_TARGET_TOKEN"));
    }

    @Test
    void shouldCreateMatchBetweenCandidateAndJob() throws Exception {
        int candidateId = candidateIdToUse();

        String pipelines = targetService.getJobPipelines();
        log.info("=== RESPONSE GET /job-pipelines/ ===");
        log.info(pipelines);

        String jobJson = targetService.getTargetJob(JOB_ID);
        log.info("=== RESPONSE GET /jobs/{}/ ===", JOB_ID);
        log.info(jobJson);
        int jobPipelineId = objectMapper.readTree(jobJson).path("job_pipeline").asInt(0);
        log.info("Job {} pipeline id = {} (0 = field absent)", JOB_ID, jobPipelineId);

        log.info("=== PAYLOAD SENT TO POST /matches/ ===");
        log.info(objectMapper.writeValueAsString(Map.of("job", JOB_ID, "candidate", candidateId)));

        String matchResponse = targetService.createCandidateMatch(candidateId, JOB_ID);
        log.info("=== RESPONSE POST /matches/ (job {}, candidate {}) ===", JOB_ID, candidateId);
        log.info(matchResponse);

        long matchId = objectMapper.readTree(matchResponse).path("id").asLong();
        assertTrue(matchId > 0, "Expected a match id in response: " + matchResponse);

        Integer stageId = stageIdForJob(pipelines, jobPipelineId);
        if (stageId != null) {
            log.info("=== PAYLOAD SENT TO PATCH /matches/{}/ ===", matchId);
            log.info(objectMapper.writeValueAsString(Map.of("job_pipeline_stage", Map.of("id", stageId))));
            String patchResponse = targetService.updateMatchStage((int) matchId, stageId);
            log.info("=== RESPONSE PATCH /matches/{}/ (stage set to {}) ===", matchId, stageId);
            log.info(patchResponse);
        } else {
            log.warn("No stage found, skipping stage update on match {}", matchId);
        }

        String matches = targetService.getCandidateMatches(candidateId);
        log.info("=== RESPONSE GET /candidates/{}/matches/ ===", candidateId);
        log.info(matches);
    }

    private int candidateIdToUse() throws Exception {
        String existing = System.getenv("MANATAL_CANDIDATE_ID");
        if (existing != null && !existing.isBlank()) {
            log.info("Reusing existing target candidate {}", existing);
            return Integer.parseInt(existing.trim());
        }

        CandidateSource source = new CandidateSource();
        source.setFullName("[MIGRATION-TEST] Match Mock");
        source.setCandidateLocation("Porto, Portugal");
        source.setEmail("match.mock@example.com");
        source.setPhoneNumber("+351 910 000 000");
        source.setDescription("<p>Candidato mock para validar POST /matches/.</p>");
        source.setConsent(true);

        String response = targetService.migrateCandidate(candidateMapper.toTarget(source));
        log.info("=== RESPONSE POST /candidates/ (match mock) ===");
        log.info(response);
        return (int) objectMapper.readTree(response).path("id").asLong();
    }

    private Integer stageIdForJob(String pipelinesJson, int jobPipelineId) throws Exception {
        var root = objectMapper.readTree(pipelinesJson);
        var results = root.has("results") ? root.get("results") : root;
        if (!results.isArray()) {
            return null;
        }

        var selected = jobPipelineId > 0 ? findPipeline(results, jobPipelineId) : null;
        if (selected == null) {
            selected = findPipelineByName(results, "Default Job Pipeline");
        }
        if (selected == null) {
            selected = results.size() > 0 ? results.get(0) : null;
        }
        if (selected == null) {
            log.warn("No pipeline found, posting match without stage");
            return null;
        }

        var stages = selected.get("job_pipeline_stages");
        if (stages != null && stages.isArray() && stages.size() > 0) {
            return stages.get(0).path("id").asInt();
        }
        log.warn("No stage found for pipeline {}, posting match without stage", selected.path("id").asInt());
        return null;
    }

    private JsonNode findPipeline(JsonNode results, int pipelineId) {
        for (JsonNode pipeline : results) {
            if (pipeline.path("id").asInt() == pipelineId) {
                return pipeline;
            }
        }
        return null;
    }

    private JsonNode findPipelineByName(JsonNode results, String name) {
        for (JsonNode pipeline : results) {
            if (name.equals(pipeline.path("name").asText())) {
                return pipeline;
            }
        }
        return null;
    }
}
