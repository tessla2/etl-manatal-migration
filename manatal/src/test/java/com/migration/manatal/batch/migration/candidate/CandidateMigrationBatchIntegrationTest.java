package com.migration.manatal.batch.migration.candidate;

import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.model.candidate.CandidateTarget;
import com.migration.manatal.repository.candidate.CandidateMigrationRepository;
import com.migration.manatal.service.candidate.ManatalSourceCandidateService;
import com.migration.manatal.service.candidate.ManatalSourceCandidateService.CandidateExportInfo;
import com.migration.manatal.service.candidate.ManatalTargetCandidateService;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
class CandidateMigrationBatchIntegrationTest {

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private CandidateMigrationRepository repository;

    @MockitoBean
    private ManatalSourceCandidateService sourceService;

    @MockitoBean
    private ManatalTargetCandidateService targetService;

    @Test
    void shouldRunFullCandidateBatchOnceWithTwoMockCandidates() throws Exception {
        when(sourceService.listCandidatesWithExportFilter()).thenReturn(List.of(
                new CandidateExportInfo("201", "Ana Silva"),
                new CandidateExportInfo("202", "Bruno Souza")));

        when(sourceService.previewCandidateMigrated("201")).thenReturn(candidateTarget("Ana Silva"));
        when(sourceService.previewCandidateMigrated("202")).thenReturn(candidateTarget("Bruno Souza"));

        when(sourceService.getCandidateMatches(any())).thenReturn(List.of());
        when(sourceService.getCandidateResume(any())).thenReturn(null);
        when(sourceService.getCandidateAttachments(any())).thenReturn(List.of());

        when(targetService.migrateCandidate(any())).thenAnswer(invocation -> {
            CandidateTarget target = invocation.getArgument(0);
            long id = target.getFullName().endsWith("Silva") ? 501 : 502;
            return "{\"id\": " + id + "}";
        });

        long executionId = jobOperator.start("candidateMigrationJob", new Properties());
        assertTrue(executionId > 0, "Expected a job execution id");

        List<CandidateMigration> all = repository.findAll();
        assertEquals(2, all.size());
        for (CandidateMigration migration : all) {
            assertEquals("SUCESSO", migration.getStatus());
            assertNotNull(migration.getTargetCandidateId());
        }

        verify(targetService, times(2)).migrateCandidate(any());
    }

    private CandidateTarget candidateTarget(String name) {
        CandidateTarget target = new CandidateTarget();
        target.setFullName(name);
        return target;
    }
}
