package com.migration.manatal.sandbox;

import com.migration.manatal.config.OwnerMappingProperties;
import com.migration.manatal.model.job.JobSource;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.service.ManatalApiClient;
import com.migration.manatal.service.job.ManatalTargetJobService;
import com.migration.manatal.transform.OwnerMapper;
import com.migration.manatal.transform.JobMapper;
import com.migration.manatal.transform.IndustryMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@EnabledIfEnvironmentVariable(named = "MANATAL_TARGET_TOKEN", matches = ".+")
class ManatalPostSandboxJobTest {

    private static final String BASE_URL = System.getenv().getOrDefault("MANATAL_BASE_URL", "https://api.manatal.com/open/v3/");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JobMapper jobMapper;
    private ManatalTargetJobService targetService;

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

        targetService = new ManatalTargetJobService(apiClient);
        jobMapper = new JobMapper(new OwnerMapper(new OwnerMappingProperties()),
                new IndustryMapper(targetService));

        var tokenField = ManatalTargetJobService.class.getDeclaredField("targetToken");
        tokenField.setAccessible(true);
        tokenField.set(targetService, System.getenv("MANATAL_TARGET_TOKEN"));
    }

    @Test
    void shouldPostMockJobAndNoteToTarget() throws Exception {
        JobSource source = objectMapper.readValue(SandboxJobSample.SAMPLE_SOURCE_JOB, JobSource.class);

        JobSource.JobNote note = new JobSource.JobNote();
        note.setContent("Nota de teste: escrita no dia 31/07 para validar o POST de notas.");
        note.setCreator(1);
        note.setCreatedAt("2026-07-31T10:00:00Z");

        JobTarget target = jobMapper.toTarget(source, List.of(note));
        target.setPositionName("[MIGRATION-TEST] " + target.getPositionName());
        target.setHeadcount(1);

        log.info("=== PAYLOAD SENT TO POST /jobs/ ===");
        log.info(objectMapper.writeValueAsString(target));

        String response = targetService.migrateJob(target);
        log.info("=== RESPONSE POST /jobs/ ===");
        log.info(response);

        long targetJobId = objectMapper.readTree(response).path("id").asLong();
        assertTrue(targetJobId > 0, "Expected a target job id in response: " + response);

        log.info("=== PAYLOAD SENT TO POST /jobs/{}/notes/ ===", targetJobId);
        log.info("{\"info\": \"Nota de validacao pos-criacao.\"}");

        String noteResponse = targetService.createJobNote((int) targetJobId, "Nota de validacao pos-criacao.");
        log.info("=== RESPONSE POST note (job {}) ===", targetJobId);
        log.info(noteResponse);
    }
}
