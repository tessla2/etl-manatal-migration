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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@EnabledIfEnvironmentVariable(named = "MANATAL_TARGET_TOKEN", matches = ".+")
class ManatalPostSandboxDropTest {

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
    void shouldCreateThreeMatchesWithTwoStagesAndOneDropped() throws Exception {
        String pipelines = targetService.getJobPipelines();
        log.info("=== RESPONSE GET /job-pipelines/ ===");
        log.info(pipelines);

        String jobJson = targetService.getTargetJob(JOB_ID);
        log.info("=== RESPONSE GET /jobs/{}/ ===", JOB_ID);
        log.info(jobJson);
        int jobPipelineId = objectMapper.readTree(jobJson).path("job_pipeline").asInt(0);

        List<Integer> stages = stagesForJob(pipelines, jobPipelineId);
        if (stages.size() < 2) {
            log.warn("Job's pipeline has {} stage(s); expected 2. Using what is available.", stages.size());
        }
        log.info("Stages to use for job {}: {}", JOB_ID, stages);

        int candidate1 = createCandidate("Drop 1");
        int candidate2 = createCandidate("Drop 2");
        int candidate3 = createCandidate("Drop 3");

        long match1 = createMatch(candidate1);
        long match2 = createMatch(candidate2);
        long match3 = createMatch(candidate3);

        if (!stages.isEmpty()) {
            setStage(match1, stages.get(0));
        }
        if (stages.size() > 1) {
            setStage(match2, stages.get(1));
        }
        if (!stages.isEmpty()) {
            setStage(match3, stages.get(0));
            dropWithNote(match3);
        } else {
            dropWithNote(match3);
        }

        verifyCandidate(candidate1, "Candidate 1", stages.size() > 0 ? stages.get(0) : null, true, null);
        verifyCandidate(candidate2, "Candidate 2", stages.size() > 1 ? stages.get(1) : null, true, null);
        verifyCandidate(candidate3, "Candidate 3 (dropped)", stages.size() > 0 ? stages.get(0) : null, false, true);
    }

    private int createCandidate(String label) throws Exception {
        CandidateSource source = new CandidateSource();
        source.setFullName("[MIGRATION-TEST] " + label);
        source.setCandidateLocation("Porto, Portugal");
        source.setEmail("drop." + label.toLowerCase().replace(" ", "") + "@example.com");
        source.setPhoneNumber("+351 910 000 000");
        source.setDescription("<p>Candidato mock para validar match com stages e drop.</p>");
        source.setConsent(true);

        String response = targetService.migrateCandidate(candidateMapper.toTarget(source));
        log.info("=== RESPONSE POST /candidates/ ({}) ===", label);
        log.info(response);
        return (int) objectMapper.readTree(response).path("id").asLong();
    }

    private long createMatch(int candidateId) throws Exception {
        String response = targetService.createCandidateMatch(candidateId, JOB_ID);
        log.info("=== RESPONSE POST /matches/ (candidate {}) ===", candidateId);
        log.info(response);
        long matchId = objectMapper.readTree(response).path("id").asLong();
        assertTrue(matchId > 0, "Expected a match id in response: " + response);
        return matchId;
    }

    private void setStage(long matchId, int stageId) throws Exception {
        log.info("=== PATCH /matches/{}/ → job_pipeline_stage {} ===", matchId, stageId);
        String response = targetService.updateMatchStage((int) matchId, stageId);
        log.info(response);
    }

    private void dropMatch(long matchId) throws Exception {
        String droppedAt = LocalDateTime.now().toString();
        log.info("=== PATCH /matches/{}/ → is_active false, dropped_at {} ===", matchId, droppedAt);
        String response = targetService.dropMatch((int) matchId, droppedAt);
        log.info(response);
    }

    private void dropWithNote(long matchId) throws Exception {
        String droppedAt = LocalDateTime.now().toString();
        log.info("=== PATCH /matches/{}/ → is_active false, dropped_at {} ===", matchId, droppedAt);
        String dropResponse = targetService.dropMatch((int) matchId, droppedAt);
        log.info(dropResponse);

        String stageName = objectMapper.readTree(dropResponse)
                .path("job_pipeline_stage").path("name").asText();
        String noteContent = "Dropado em " + droppedAt + " do stage " + stageName;
        log.info("=== POST /matches/{}/notes/ → {} ===", matchId, noteContent);
        String noteResponse = targetService.createMatchNote((int) matchId, noteContent);
        log.info(noteResponse);
    }

    private void verifyCandidate(int candidateId, String label, Integer expectedStage, Boolean active, Boolean dropped)
            throws Exception {
        String matches = targetService.getCandidateMatches(candidateId);
        log.info("=== RESPONSE GET /candidates/{}/matches/ ({}) ===", candidateId, label);
        log.info(matches);

        JsonNode match = findMatchForJob(matches, JOB_ID);
        assertNotNull(match, label + ": expected a match for job " + JOB_ID);

        if (expectedStage != null) {
            assertEquals(expectedStage, match.path("job_pipeline_stage").path("id").asInt(),
                    label + ": unexpected stage");
        }
        if (active != null) {
            assertEquals(active, match.path("is_active").asBoolean(), label + ": unexpected is_active");
        }
        if (Boolean.TRUE.equals(dropped)) {
            assertFalse(match.path("dropped_at").isNull(), label + ": expected dropped_at to be set");
        }
    }

    private List<Integer> stagesForJob(String pipelinesJson, int jobPipelineId) throws Exception {
        List<Integer> stages = new ArrayList<>();
        var root = objectMapper.readTree(pipelinesJson);
        var results = root.has("results") ? root.get("results") : root;
        if (!results.isArray()) {
            return stages;
        }

        var selected = jobPipelineId > 0 ? findPipeline(results, jobPipelineId) : null;
        if (selected == null) {
            selected = findPipelineByName(results, "Default Job Pipeline");
        }
        if (selected == null && results.size() > 0) {
            selected = results.get(0);
        }
        if (selected == null) {
            return stages;
        }

        var stageNodes = selected.get("job_pipeline_stages");
        if (stageNodes != null && stageNodes.isArray()) {
            for (JsonNode stage : stageNodes) {
                stages.add(stage.path("id").asInt());
            }
        }
        return stages;
    }

    private JsonNode findMatchForJob(String matchesJson, int jobId) throws Exception {
        var root = objectMapper.readTree(matchesJson);
        var results = root.has("results") ? root.get("results") : root;
        if (!results.isArray()) {
            return null;
        }
        for (JsonNode match : results) {
            if (match.path("job").asInt() == jobId) {
                return match;
            }
        }
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
