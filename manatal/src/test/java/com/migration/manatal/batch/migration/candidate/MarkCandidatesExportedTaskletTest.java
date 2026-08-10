package com.migration.manatal.batch.migration.candidate;

import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.repository.candidate.CandidateMigrationRepository;
import com.migration.manatal.service.candidate.ManatalSourceCandidateService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

@ExtendWith(MockitoExtension.class)
class MarkCandidatesExportedTaskletTest {

    @Mock
    private CandidateMigrationRepository repository;

    @Mock
    private ManatalSourceCandidateService sourceService;

    private MarkCandidatesExportedTasklet newTasklet() {
        return new MarkCandidatesExportedTasklet(repository, sourceService);
    }

    private CandidateMigration candidate(Long id, String sourceId, String status, Boolean tagged) {
        CandidateMigration c = new CandidateMigration();
        c.setId(id);
        c.setSourceCandidateId(sourceId);
        c.setStatus(status);
        c.setTaggedInSource(tagged);
        return c;
    }

    @Test
    void shouldTagAndRemoveToExportAndMarkSuccessCandidates() throws Exception {
        var c1 = candidate(1L, "201", "SUCESSO", null);
        var c2 = candidate(2L, "202", "SUCESSO", true);
        when(repository.findByStatus("SUCESSO")).thenReturn(List.of(c1, c2));
        when(sourceService.getCandidateTagId("201", "To Export")).thenReturn(null);

        var result = newTasklet().execute(null, null);

        assertEquals(RepeatStatus.FINISHED, result);
        verify(sourceService).addCandidateTag("201", "Exported");
        verify(sourceService, never()).removeCandidateTag(anyString(), any());
        verify(sourceService).updateCustomField("201", "exported", "Yes");
        assertEquals(Boolean.TRUE, c1.getTaggedInSource());
        verify(repository).save(c1);
        verify(sourceService, never()).addCandidateTag("202", "Exported");
    }

    @Test
    void shouldRemoveToExportTagWhenPresent() throws Exception {
        var c1 = candidate(1L, "201", "SUCESSO", null);
        when(repository.findByStatus("SUCESSO")).thenReturn(List.of(c1));
        when(sourceService.getCandidateTagId("201", "To Export")).thenReturn(987L);

        newTasklet().execute(null, null);

        verify(sourceService).addCandidateTag("201", "Exported");
        verify(sourceService).removeCandidateTag("201", 987L);
        verify(sourceService).updateCustomField("201", "exported", "Yes");
        assertEquals(Boolean.TRUE, c1.getTaggedInSource());
        verify(repository).save(c1);
    }

    @Test
    void shouldContinueOnFailureWithoutFlagging() throws Exception {
        var c1 = candidate(1L, "201", "SUCESSO", null);
        var c2 = candidate(2L, "202", "SUCESSO", null);
        when(repository.findByStatus("SUCESSO")).thenReturn(List.of(c1, c2));
        when(sourceService.getCandidateTagId(anyString(), anyString())).thenReturn(null);
        doThrow(new RuntimeException("boom")).when(sourceService).updateCustomField("201", "exported", "Yes");

        newTasklet().execute(null, null);

        assertNull(c1.getTaggedInSource());
        verify(repository, never()).save(c1);
        verify(sourceService).updateCustomField("202", "exported", "Yes");
        assertEquals(Boolean.TRUE, c2.getTaggedInSource());
        verify(repository).save(c2);
    }

    @Test
    void shouldDoNothingWhenNoCandidates() throws Exception {
        when(repository.findByStatus("SUCESSO")).thenReturn(List.of());

        newTasklet().execute(null, null);

        verify(sourceService, never()).addCandidateTag(anyString(), anyString());
        verify(sourceService, never()).getCandidateTagId(anyString(), anyString());
        verify(sourceService, never()).removeCandidateTag(anyString(), any());
        verify(sourceService, never()).updateCustomField(anyString(), anyString(), anyString());
        verify(repository, never()).save(any());
    }
}
