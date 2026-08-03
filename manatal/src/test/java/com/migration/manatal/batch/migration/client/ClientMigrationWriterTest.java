package com.migration.manatal.batch.migration.client;

import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import com.migration.manatal.service.client.ManatalSourceClientService;
import com.migration.manatal.service.client.ManatalTargetClientService;
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
class ClientMigrationWriterTest {

    @Mock
    private ManatalTargetClientService targetService;

    @Mock
    private ManatalSourceClientService sourceService;

    @Mock
    private ClientMigrationRepository repository;

    @Captor
    private ArgumentCaptor<ClientMigration> entityCaptor;

    private ClientMigrationWriter writer;

    @BeforeEach
    void setUp() {
        writer = new ClientMigrationWriter(targetService, sourceService, repository, new ObjectMapper());
    }

    @Test
    void shouldWriteSuccessfully() throws Exception {
        var entity = new ClientMigration();
        entity.setSourceOrganizationId("42");
        entity.setStatus("PENDENTE");

        var target = new ClientTarget();
        target.setClientName("Acme Corp");
        var contact = new ClientTarget.ContactTarget();
        contact.setFullName("John Doe");
        target.setContacts(List.of(contact));
        var note = new ClientTarget.TargetNote();
        note.setContent("hello");
        target.setNotes(List.of(note));

        var pkg = new ClientMigrationPackage();
        pkg.setEntity(entity);
        pkg.setTransformed(target);

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateOrganization(target)).thenReturn("{\"id\": 55}");

        writer.write(new Chunk<>(pkg));

        verify(targetService).migrateOrganization(target);
        verify(targetService).createContact(contact);
        verify(targetService).createOrganizationNote(55, "hello");
        verify(sourceService).updateCustomField("42", "exported", "Yes");
        verify(repository, times(2)).save(entityCaptor.capture());

        var saved = entityCaptor.getAllValues();
        assertEquals("SUCESSO", saved.get(0).getStatus());
        assertEquals(55L, saved.get(0).getTargetOrganizationId());
        assertNull(saved.get(0).getErrorMessage());

        assertTrue(saved.get(1).getTaggedInSource());
    }

    @Test
    void shouldKeepSuccessWhenContactOrNotePostingFails() throws Exception {
        var entity = new ClientMigration();
        entity.setSourceOrganizationId("42");
        entity.setStatus("PENDENTE");

        var target = new ClientTarget();
        target.setClientName("Acme Corp");
        var contact = new ClientTarget.ContactTarget();
        contact.setFullName("John Doe");
        target.setContacts(List.of(contact));
        var note = new ClientTarget.TargetNote();
        note.setContent("hello");
        target.setNotes(List.of(note));

        var pkg = new ClientMigrationPackage();
        pkg.setEntity(entity);
        pkg.setTransformed(target);

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateOrganization(target)).thenReturn("{\"id\": 55}");
        doThrow(new RuntimeException("contact error")).when(targetService).createContact(any());
        doThrow(new RuntimeException("note error")).when(targetService).createOrganizationNote(anyInt(), anyString());

        writer.write(new Chunk<>(pkg));

        verify(targetService).createContact(contact);
        verify(targetService).createOrganizationNote(55, "hello");
        assertEquals("SUCESSO", entity.getStatus());
        assertEquals(55L, entity.getTargetOrganizationId());
    }

    @Test
    void shouldMarkErroWhenTargetFails() throws Exception {
        var entity = new ClientMigration();
        entity.setSourceOrganizationId("42");
        entity.setStatus("PENDENTE");

        var target = new ClientTarget();
        var pkg = new ClientMigrationPackage();
        pkg.setEntity(entity);
        pkg.setTransformed(target);

        doThrow(new RuntimeException("target error")).when(targetService).migrateOrganization(target);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        writer.write(new Chunk<>(pkg));

        verify(repository).save(entityCaptor.capture());
        var saved = entityCaptor.getValue();
        assertEquals("ERRO", saved.getStatus());
        assertEquals("target error", saved.getErrorMessage());
    }

    @Test
    void shouldSkipItemWithPreSetError() throws Exception {
        var entity = new ClientMigration();
        entity.setSourceOrganizationId("42");

        var pkg = new ClientMigrationPackage();
        pkg.setEntity(entity);
        pkg.setErrorMessage("previous error");

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        writer.write(new Chunk<>(pkg));

        verify(targetService, never()).migrateOrganization(any());
        verify(repository).save(entityCaptor.capture());
        assertEquals("ERRO", entityCaptor.getValue().getStatus());
        assertEquals("previous error", entityCaptor.getValue().getErrorMessage());
    }
}
