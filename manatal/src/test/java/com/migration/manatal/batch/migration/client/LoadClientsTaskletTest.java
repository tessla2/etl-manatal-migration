package com.migration.manatal.batch.migration.client;

import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import com.migration.manatal.service.client.ManatalSourceClientService;
import com.migration.manatal.service.client.ManatalSourceClientService.OrganizationExportInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoadClientsTaskletTest {

    @Mock
    private ManatalSourceClientService sourceService;

    @Mock
    private ClientMigrationRepository repository;

    @Captor
    private ArgumentCaptor<ClientMigration> entityCaptor;

    private LoadClientsTasklet tasklet;

    @BeforeEach
    void setUp() {
        tasklet = new LoadClientsTasklet(sourceService, repository);
    }

    @Test
    void shouldLoadNewClients() throws Exception {
        when(sourceService.listOrganizationsWithExportFilter()).thenReturn(List.of(
                new OrganizationExportInfo("1", "Acme Corp"),
                new OrganizationExportInfo("2", "Beta Inc")));
        when(repository.findBySourceOrganizationId(any())).thenReturn(Optional.empty());

        var result = tasklet.execute(null, null);

        assertEquals(org.springframework.batch.infrastructure.repeat.RepeatStatus.FINISHED, result);
        verify(repository, times(2)).save(entityCaptor.capture());

        var saved = entityCaptor.getAllValues();
        assertEquals("1", saved.get(0).getSourceOrganizationId());
        assertEquals("Acme Corp", saved.get(0).getSourceName());
        assertEquals("PENDENTE", saved.get(0).getStatus());
        assertEquals("2", saved.get(1).getSourceOrganizationId());
        assertEquals("Beta Inc", saved.get(1).getSourceName());
    }

    @Test
    void shouldSkipExistingClients() throws Exception {
        when(sourceService.listOrganizationsWithExportFilter()).thenReturn(List.of(
                new OrganizationExportInfo("1", "Acme Corp")));
        var existing = new ClientMigration();
        existing.setSourceOrganizationId("1");
        when(repository.findBySourceOrganizationId("1")).thenReturn(Optional.of(existing));

        tasklet.execute(null, null);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldLoadOnlyNewClientsWhenMixExists() throws Exception {
        when(sourceService.listOrganizationsWithExportFilter()).thenReturn(List.of(
                new OrganizationExportInfo("1", "Acme Corp"),
                new OrganizationExportInfo("2", "Beta Inc")));
        var existing = new ClientMigration();
        existing.setSourceOrganizationId("1");
        when(repository.findBySourceOrganizationId("1")).thenReturn(Optional.of(existing));
        when(repository.findBySourceOrganizationId("2")).thenReturn(Optional.empty());

        tasklet.execute(null, null);

        verify(repository, times(1)).save(entityCaptor.capture());
        assertEquals("2", entityCaptor.getValue().getSourceOrganizationId());
    }

    @Test
    void shouldStopWhenEmptyResults() throws Exception {
        when(sourceService.listOrganizationsWithExportFilter()).thenReturn(List.of());

        tasklet.execute(null, null);

        verify(repository, never()).save(any());
    }
}
