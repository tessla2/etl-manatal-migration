package com.migration.manatal.batch.migration.job;

import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import com.migration.manatal.service.job.ManatalSourceJobService;
import com.migration.manatal.transform.JobMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobMigrationProcessorTest {

    @Mock
    private ManatalSourceJobService sourceJobService;

    @Mock
    private JobMapper jobMapper;

    @Mock
    private ClientMigrationRepository clientMigrationRepository;

    private JobMigrationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new JobMigrationProcessor(sourceJobService, jobMapper, clientMigrationRepository);
    }

    @Test
    void shouldResolveOrganizationToTargetClient() {
        var entity = new JobMigration();
        entity.setSourceJobId("42");
        entity.setStatus("PENDENTE");

        var target = new JobTarget();
        target.setPositionName("Dev");
        target.setOrganization(100);
        when(sourceJobService.previewJobMigrated("42")).thenReturn(target);

        var client = new ClientMigration();
        client.setSourceOrganizationId("100");
        client.setTargetOrganizationId(9001L);
        when(clientMigrationRepository.findBySourceOrganizationId("100")).thenReturn(Optional.of(client));

        var result = processor.process(entity);

        assertNotNull(result);
        assertNull(result.getErrorMessage());
        assertEquals(9001, result.getTransformed().getOrganization());
        assertEquals("100", result.getEntity().getSourceOrganizationId());
        assertEquals(9001L, result.getEntity().getTargetOrganizationId());
    }

    @Test
    void shouldLeaveOrganizationEmptyWhenSourceOrgNotMigrated() {
        var entity = new JobMigration();
        entity.setSourceJobId("42");
        entity.setStatus("PENDENTE");

        var target = new JobTarget();
        target.setPositionName("Dev");
        target.setOrganization(777);
        when(sourceJobService.previewJobMigrated("42")).thenReturn(target);

        when(clientMigrationRepository.findBySourceOrganizationId("777")).thenReturn(Optional.empty());

        var result = processor.process(entity);

        assertNotNull(result);
        assertNull(result.getTransformed().getOrganization());
        assertEquals("777", result.getEntity().getSourceOrganizationId());
        assertNull(result.getEntity().getTargetOrganizationId());
    }

    @Test
    void shouldSkipNonPendenteItem() {
        var entity = new JobMigration();
        entity.setStatus("SUCESSO");

        var result = processor.process(entity);

        assertNull(result);
    }

    @Test
    void shouldCaptureErrorMessageOnFailure() {
        var entity = new JobMigration();
        entity.setSourceJobId("99");
        entity.setStatus("PENDENTE");

        when(sourceJobService.previewJobMigrated("99"))
                .thenThrow(new RuntimeException("API error"));

        var result = processor.process(entity);

        assertNotNull(result);
        assertEquals(entity, result.getEntity());
        assertNull(result.getTransformed());
        assertEquals("API error", result.getErrorMessage());
    }
}
