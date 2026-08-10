package com.migration.manatal.batch.migration.job;

import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.repository.job.JobMigrationRepository;
import com.migration.manatal.service.job.ManatalSourceJobService;
import com.migration.manatal.service.job.ManatalSourceJobService.JobExportInfo;
import com.migration.manatal.service.job.ManatalTargetJobService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
class JobMigrationBatchIntegrationTest {

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private JobMigrationRepository repository;

    @MockitoBean
    private ManatalSourceJobService sourceService;

    @MockitoBean
    private ManatalTargetJobService targetService;

    @Test
    void shouldRunFullJobBatchOnceWithTwoMockJobs() throws Exception {
        when(sourceService.listJobWithExportedFilter()).thenReturn(List.of(
                new JobExportInfo("101", "Mock Job A"),
                new JobExportInfo("102", "Mock Job B")));

        when(sourceService.previewJobMigrated("101")).thenReturn(jobTarget("Mock Job A"));
        when(sourceService.previewJobMigrated("102")).thenReturn(jobTarget("Mock Job B"));

        when(targetService.migrateJob(any())).thenAnswer(invocation -> {
            JobTarget target = invocation.getArgument(0);
            long id = target.getPositionName().endsWith("A") ? 501 : 502;
            return "{\"id\": " + id + "}";
        });

        long executionId = jobOperator.start("jobMigrationJob", new Properties());
        assertTrue(executionId > 0, "Expected a job execution id");

        List<JobMigration> all = repository.findAll();
        assertEquals(2, all.size());
        for (JobMigration migration : all) {
            assertEquals("SUCESSO", migration.getStatus());
            assertNotNull(migration.getTargetJobId());
        }

        verify(targetService, times(2)).migrateJob(any());
        verify(sourceService, times(2)).updateCustomField(any(), eq("exported"), eq("Yes"));
    }

    private JobTarget jobTarget(String name) {
        JobTarget target = new JobTarget();
        target.setPositionName(name);
        target.setOrganization(1);
        return target;
    }
}
