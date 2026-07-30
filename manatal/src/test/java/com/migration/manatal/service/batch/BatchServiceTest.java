package com.migration.manatal.service.batch;

import com.migration.manatal.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private JobOperator jobOperator;

    private BatchService service;

    @BeforeEach
    void setUp() {
        service = new BatchService(jobOperator);
    }

    @Test
    void shouldStartClientJob() throws Exception {
        when(jobOperator.start(eq("clientMigrationJob"), any())).thenReturn(42L);

        var executionId = service.startJob("CLIENT");

        assertEquals(42L, executionId);
    }

    @Test
    void shouldThrowOnUnknownType() {
        var ex = assertThrows(ApiException.class, () -> service.startJob("UNKNOWN"));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void shouldThrowNotFoundWhenJobMissing() throws Exception {
        when(jobOperator.start(eq("clientMigrationJob"), any())).thenThrow(new NoSuchJobException("not found"));

        var ex = assertThrows(ApiException.class, () -> service.startJob("CLIENT"));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void shouldThrowConflictWhenJobAlreadyRunning() throws Exception {
        when(jobOperator.start(eq("clientMigrationJob"), any()))
                .thenThrow(new JobInstanceAlreadyExistsException("already exists"));

        var ex = assertThrows(ApiException.class, () -> service.startJob("CLIENT"));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void shouldThrowConflictOnInvalidParameters() throws Exception {
        when(jobOperator.start(eq("clientMigrationJob"), any()))
                .thenThrow(new InvalidJobParametersException("invalid"));

        var ex = assertThrows(ApiException.class, () -> service.startJob("CLIENT"));
        assertEquals(409, ex.getStatus().value());
    }

    @Test
    void shouldReturnSummary() throws Exception {
        when(jobOperator.getSummary(1L)).thenReturn("summary text");

        var result = service.getSummary(1L);

        assertEquals("summary text", result);
    }

    @Test
    void shouldThrowNotFoundForMissingExecution() throws Exception {
        when(jobOperator.getSummary(99L)).thenThrow(new NoSuchJobExecutionException("not found"));

        var ex = assertThrows(ApiException.class, () -> service.getSummary(99L));
        assertEquals(404, ex.getStatus().value());
    }
}
