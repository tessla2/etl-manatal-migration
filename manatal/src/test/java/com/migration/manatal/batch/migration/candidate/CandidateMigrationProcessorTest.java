package com.migration.manatal.batch.migration.candidate;

import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.model.candidate.CandidateTarget;
import com.migration.manatal.service.candidate.ManatalSourceCandidateService;
import com.migration.manatal.transform.CandidateMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateMigrationProcessorTest {

    @Mock
    private ManatalSourceCandidateService sourceCandidateService;

    @Mock
    private CandidateMapper candidateMapper;

    private CandidateMigrationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new CandidateMigrationProcessor(sourceCandidateService, candidateMapper);
    }

    @Test
    void shouldProcessPendenteItem() {
        var entity = new CandidateMigration();
        entity.setSourceCandidateId("42");
        entity.setStatus("PENDENTE");

        var target = new CandidateTarget();
        target.setFullName("Ana Silva");
        when(sourceCandidateService.previewCandidateMigrated("42")).thenReturn(target);

        var result = processor.process(entity);

        assertNotNull(result);
        assertEquals(entity, result.getEntity());
        assertEquals(target, result.getTransformed());
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldSkipNonPendenteItem() {
        var entity = new CandidateMigration();
        entity.setStatus("SUCESSO");

        var result = processor.process(entity);

        assertNull(result);
    }

    @Test
    void shouldCaptureErrorMessageOnFailure() {
        var entity = new CandidateMigration();
        entity.setSourceCandidateId("99");
        entity.setStatus("PENDENTE");

        when(sourceCandidateService.previewCandidateMigrated("99"))
                .thenThrow(new RuntimeException("API error"));

        var result = processor.process(entity);

        assertNotNull(result);
        assertEquals(entity, result.getEntity());
        assertNull(result.getTransformed());
        assertEquals("API error", result.getErrorMessage());
    }
}
