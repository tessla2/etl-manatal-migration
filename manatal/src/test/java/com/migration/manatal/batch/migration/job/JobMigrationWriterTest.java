package com.migration.manatal.batch.migration.job;

import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.repository.job.JobMigrationRepository;
import com.migration.manatal.service.job.ManatalSourceJobService;
import com.migration.manatal.service.job.ManatalTargetJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobMigrationWriterTest {

    @Mock
    private ManatalTargetJobService targetService;

    @Mock
    private ManatalSourceJobService sourceService;

    @Mock
    private JobMigrationRepository repository;

    @Captor
    private ArgumentCaptor<JobMigration> entityCaptor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private JobMigrationWriter writer;

    @BeforeEach
    void setUp() {
        writer = new JobMigrationWriter(targetService, sourceService, repository, objectMapper);
    }

    @Test
    void shouldWriteSuccessfullyAndPostNotes() throws Exception {
        var entity = new JobMigration();
        entity.setSourceJobId("42");
        entity.setStatus("PENDENTE");

        var target = new JobTarget();
        target.setPositionName("Dev");
        var note = new JobTarget.TargetNote();
        note.setContent("nota escrita a mao");
        target.setNotes(List.of(note));

        var pkg = new JobMigrationPackage();
        pkg.setEntity(entity);
        pkg.setTransformed(target);

        when(targetService.migrateJob(target)).thenReturn("{\"id\": 55}");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        writer.write(new Chunk<>(pkg));

        verify(targetService).createJobNote(55, "nota escrita a mao");
        verify(sourceService).updateCustomField("42", "exported", "Yes");
        verify(repository, times(2)).save(entityCaptor.capture());

        var saved = entityCaptor.getAllValues();
        assertEquals("SUCESSO", saved.get(0).getStatus());
        assertEquals(55L, saved.get(0).getTargetJobId());
        assertNull(saved.get(0).getErrorMessage());
        assertTrue(saved.get(1).getTaggedInSource());
    }

    @Test
    void shouldKeepSucessoWhenNotePostFails() throws Exception {
        var entity = new JobMigration();
        entity.setSourceJobId("42");
        entity.setStatus("PENDENTE");

        var target = new JobTarget();
        var note = new JobTarget.TargetNote();
        note.setContent("nota 1");
        target.setNotes(List.of(note));

        var pkg = new JobMigrationPackage();
        pkg.setEntity(entity);
        pkg.setTransformed(target);

        when(targetService.migrateJob(target)).thenReturn("{\"id\": 55}");
        doThrow(new RuntimeException("note error")).when(targetService).createJobNote(55, "nota 1");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        writer.write(new Chunk<>(pkg));

        verify(targetService).createJobNote(55, "nota 1");
        verify(repository, times(2)).save(entityCaptor.capture());
        var saved = entityCaptor.getAllValues();
        assertEquals("SUCESSO", saved.get(1).getStatus());
        assertNull(saved.get(1).getErrorMessage());
    }

    @Test
    void shouldMarkErroWhenTargetFails() throws Exception {
        var entity = new JobMigration();
        entity.setSourceJobId("42");
        entity.setStatus("PENDENTE");

        var target = new JobTarget();
        var pkg = new JobMigrationPackage();
        pkg.setEntity(entity);
        pkg.setTransformed(target);

        doThrow(new RuntimeException("target error")).when(targetService).migrateJob(target);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        writer.write(new Chunk<>(pkg));

        verify(repository).save(entityCaptor.capture());
        assertEquals("ERRO", entityCaptor.getValue().getStatus());
        assertEquals("target error", entityCaptor.getValue().getErrorMessage());
    }

    @Test
    void shouldMarkErroWhenTargetIdNotParsable() throws Exception {
        var entity = new JobMigration();
        entity.setSourceJobId("42");

        var target = new JobTarget();
        var pkg = new JobMigrationPackage();
        pkg.setEntity(entity);
        pkg.setTransformed(target);

        when(targetService.migrateJob(target)).thenReturn("{not json}");
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        writer.write(new Chunk<>(pkg));

        verify(repository).save(entityCaptor.capture());
        assertEquals("ERRO", entityCaptor.getValue().getStatus());
        assertTrue(entityCaptor.getValue().getErrorMessage().contains("parse target id"));
    }

    @Test
    void shouldSkipItemWithPreSetError() throws Exception {
        var entity = new JobMigration();
        entity.setSourceJobId("42");

        var pkg = new JobMigrationPackage();
        pkg.setEntity(entity);
        pkg.setErrorMessage("previous error");

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        writer.write(new Chunk<>(pkg));

        verify(targetService, never()).migrateJob(any());
        verify(repository).save(entityCaptor.capture());
        assertEquals("ERRO", entityCaptor.getValue().getStatus());
        assertEquals("previous error", entityCaptor.getValue().getErrorMessage());
    }
}
