package com.migration.manatal.controller.batch;

import com.migration.manatal.exception.ApiException;
import com.migration.manatal.service.batch.BatchService;
import com.migration.manatal.service.batch.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchControllerTest {

    @Mock
    private BatchService batchService;

    @Mock
    private ReportService reportService;

    private BatchController controller;

    @BeforeEach
    void setUp() {
        controller = new BatchController(batchService, reportService);
    }

    @Test
    void shouldStartClientJob() {
        when(batchService.startJob("CLIENT")).thenReturn(42L);

        var response = controller.startJob("CLIENT");

        assertEquals(200, response.getStatusCode().value());
        var body = response.getBody();
        assertEquals(42L, body.get("executionId"));
        assertEquals("STARTED", body.get("status"));
    }

    @Test
    void shouldHandleUnknownType() {
        when(batchService.startJob("UNKNOWN"))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "Unknown job type: UNKNOWN"));

        var ex = assertThrows(ApiException.class, () -> controller.startJob("UNKNOWN"));
        assertEquals(400, ex.getStatus().value());
    }

    @Test
    void shouldReturnExecutionSummary() {
        when(batchService.getSummary(1L)).thenReturn("summary text");

        var response = controller.getExecution(1L);

        assertEquals(200, response.getStatusCode().value());
        var body = response.getBody();
        assertEquals(1L, body.get("executionId"));
        assertEquals("summary text", body.get("summary"));
    }

    @Test
    void shouldHandleMissingExecution() {
        when(batchService.getSummary(99L))
                .thenThrow(ApiException.notFound("Execution not found: 99"));

        var ex = assertThrows(ApiException.class, () -> controller.getExecution(99L));
        assertEquals(404, ex.getStatus().value());
    }

    @Test
    void shouldReturnReport() {
        when(reportService.generateSummary()).thenReturn(Map.of("total", 10, "sucesso", 8));

        var response = controller.getReport();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(10, response.getBody().get("total"));
        assertEquals(8, response.getBody().get("sucesso"));
    }

    @Test
    void shouldReturnErrorsWithDefaultLimit() {
        when(reportService.listErrors(50)).thenReturn(List.of(Map.of("tipo", "CANDIDATE")));

        var response = controller.getErrors(50);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("CANDIDATE", response.getBody().get(0).get("tipo"));
    }
}
