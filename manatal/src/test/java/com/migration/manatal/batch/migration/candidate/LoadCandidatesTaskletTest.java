package com.migration.manatal.batch.migration.candidate;

import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.repository.candidate.CandidateMigrationRepository;
import com.migration.manatal.service.candidate.ManatalSourceCandidateService;
import com.migration.manatal.service.candidate.ManatalSourceCandidateService.CandidateExportInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoadCandidatesTaskletTest {

    @Mock
    private ManatalSourceCandidateService sourceService;

    @Mock
    private CandidateMigrationRepository repository;

    @Captor
    private ArgumentCaptor<CandidateMigration> entityCaptor;

    private LoadCandidatesTasklet tasklet;

    @BeforeEach
    void setUp() {
        tasklet = new LoadCandidatesTasklet(sourceService, repository);
    }

    @Test
    void shouldLoadNewCandidates() throws Exception {
        when(sourceService.listCandidatesWithExportFilter()).thenReturn(List.of(
                new CandidateExportInfo("201", "Ana Silva"),
                new CandidateExportInfo("202", "Bruno Souza")));
        when(repository.findBySourceCandidateId(any())).thenReturn(Optional.empty());

        var result = tasklet.execute(null, null);

        assertEquals(org.springframework.batch.infrastructure.repeat.RepeatStatus.FINISHED, result);
        verify(repository, times(2)).save(entityCaptor.capture());

        var saved = entityCaptor.getAllValues();
        assertEquals("201", saved.get(0).getSourceCandidateId());
        assertEquals("Ana Silva", saved.get(0).getFullName());
        assertEquals("PENDENTE", saved.get(0).getStatus());
        assertEquals("202", saved.get(1).getSourceCandidateId());
        assertEquals("Bruno Souza", saved.get(1).getFullName());
    }

    @Test
    void shouldSkipExistingCandidates() throws Exception {
        when(sourceService.listCandidatesWithExportFilter()).thenReturn(List.of(
                new CandidateExportInfo("201", "Ana Silva")));
        var existing = new CandidateMigration();
        existing.setSourceCandidateId("201");
        when(repository.findBySourceCandidateId("201")).thenReturn(Optional.of(existing));

        tasklet.execute(null, null);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldHandleNoCandidates() throws Exception {
        when(sourceService.listCandidatesWithExportFilter()).thenReturn(List.of());

        tasklet.execute(null, null);

        verify(repository, never()).save(any());
    }
}
