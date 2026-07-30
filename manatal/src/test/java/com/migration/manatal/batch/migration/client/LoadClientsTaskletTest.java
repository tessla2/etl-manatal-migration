package com.migration.manatal.batch.migration.client;

import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import com.migration.manatal.service.client.ManatalSourceClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private final ObjectMapper objectMapper = new ObjectMapper();

    private LoadClientsTasklet tasklet;

    @BeforeEach
    void setUp() {
        tasklet = new LoadClientsTasklet(sourceService, repository, objectMapper);
    }

    @Test
    void shouldLoadNewClients() throws Exception {
        var json = """
                {"results": [
                  {"id": 1, "name": "Acme Corp"},
                  {"id": 2, "name": "Beta Inc"}
                ]}
                """;
        when(sourceService.listOrganizationsWithExportFilter(0)).thenReturn(json);
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
        var json = """
                {"results": [
                  {"id": "1", "name": "Acme Corp"}
                ]}
                """;
        when(sourceService.listOrganizationsWithExportFilter(0)).thenReturn(json);
        var existing = new ClientMigration();
        existing.setSourceOrganizationId("1");
        when(repository.findBySourceOrganizationId("1")).thenReturn(Optional.of(existing));

        tasklet.execute(null, null);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldStopWhenEmptyResults() throws Exception {
        var json = """ 
                {"results": []}
                """;
        when(sourceService.listOrganizationsWithExportFilter(0)).thenReturn(json);

        tasklet.execute(null, null);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldHandlePagination() throws Exception {
        when(sourceService.listOrganizationsWithExportFilter(anyInt()))
                .thenAnswer(invocation -> {
                    int offset = invocation.getArgument(0);
                    if (offset == 0) return jsonWithNResults(100, 0);
                    if (offset == 100) return jsonWithNResults(50, 100);
                    return """
                            {"results": []}
                            """;
                });
        when(repository.findBySourceOrganizationId(any())).thenReturn(Optional.empty());

        tasklet.execute(null, null);

        verify(sourceService).listOrganizationsWithExportFilter(0);
        verify(sourceService).listOrganizationsWithExportFilter(100);
        verify(sourceService, never()).listOrganizationsWithExportFilter(200);
        verify(repository, times(150)).save(any());
    }

    private String jsonWithNResults(int count, int startId) {
        var items = IntStream.range(0, count)
                .mapToObj(i -> """
                        {"id": "%d", "name": "Org %d"}
                        """.formatted(startId + i, startId + i))
                .collect(Collectors.joining(",\n"));
        return """
                {"results": [%s]}
                """.formatted(items);
    }
}
