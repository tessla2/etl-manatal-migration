package com.migration.manatal.batch.migration.client;

import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.service.client.ManatalSourceClientService;
import com.migration.manatal.transform.ClientMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientMigrationProcessorTest {

    @Mock
    private ManatalSourceClientService sourceClientService;

    @Mock
    private ClientMapper clientMapper;

    private ClientMigrationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ClientMigrationProcessor(sourceClientService, clientMapper);
    }

    @Test
    void shouldProcessPendenteItem() {
        var entity = new ClientMigration();
        entity.setSourceOrganizationId("42");
        entity.setStatus("PENDENTE");

        var target = new ClientTarget();
        target.setClientName("Acme Corp");
        when(sourceClientService.previewClientMigrated("42")).thenReturn(target);

        var result = processor.process(entity);

        assertNotNull(result);
        assertEquals(entity, result.getEntity());
        assertEquals(target, result.getTransformed());
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldSkipNonPendenteItem() {
        var entity = new ClientMigration();
        entity.setStatus("SUCESSO");

        var result = processor.process(entity);

        assertNull(result);
    }

    @Test
    void shouldCaptureErrorMessageOnFailure() {
        var entity = new ClientMigration();
        entity.setSourceOrganizationId("99");
        entity.setStatus("PENDENTE");

        when(sourceClientService.previewClientMigrated("99"))
                .thenThrow(new RuntimeException("API error"));

        var result = processor.process(entity);

        assertNotNull(result);
        assertEquals(entity, result.getEntity());
        assertNull(result.getTransformed());
        assertEquals("API error", result.getErrorMessage());
    }
}
