package com.migration.manatal.sandbox;

import com.migration.manatal.config.OwnerMappingProperties;
import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.model.job.JobSource;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.repository.job.JobMigrationRepository;
import com.migration.manatal.service.job.ManatalSourceJobService;
import com.migration.manatal.service.job.ManatalSourceJobService.JobExportInfo;
import com.migration.manatal.transform.JobMapper;
import com.migration.manatal.transform.OwnerMapper;
import com.migration.manatal.transform.IndustryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "MANATAL_TARGET_TOKEN", matches = ".+")
class JobMigrationBatchSandboxPostTest {

    private static final String SOURCE_JOB_ID = "999991";

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private JobMigrationRepository repository;

    @Autowired
    private IndustryMapper industryMapper;

    @MockitoBean
    private ManatalSourceJobService sourceService;

    @Test
    void shouldRunBatchAndPostSamePayloadAsManatalPostSandboxJobTest() throws Exception {
        when(sourceService.listJobWithExportedFilter()).thenReturn(List.of(
                new JobExportInfo(SOURCE_JOB_ID, "Mock Software Developer")));

        ObjectMapper objectMapper = new ObjectMapper();
        JobMapper jobMapper = new JobMapper(new OwnerMapper(new OwnerMappingProperties()), industryMapper);

        JobSource source = objectMapper.readValue(SandboxJobSample.SAMPLE_SOURCE_JOB, JobSource.class);

        JobSource.JobNote note = new JobSource.JobNote();
        note.setContent("Nota de teste: escrita no dia 31/07 para validar o POST de notas.");
        note.setCreator(1);
        note.setCreatedAt("2026-07-31T10:00:00Z");

        JobTarget target = jobMapper.toTarget(source, List.of(note));
        target.setPositionName("[MIGRATION-TEST] " + target.getPositionName());
        target.setHeadcount(1);

        when(sourceService.previewJobMigrated(SOURCE_JOB_ID)).thenReturn(target);

        long executionId = jobOperator.start("jobMigrationJob", new Properties());
        assertTrue(executionId > 0, "Expected a job execution id");

        List<JobMigration> all = repository.findAll();
        assertEquals(1, all.size());
        JobMigration migration = all.get(0);
        assertEquals("SUCESSO", migration.getStatus());
        assertTrue(migration.getTargetJobId() > 0,
                "Expected a real target job id from the POST /jobs/ response");
    }
}
