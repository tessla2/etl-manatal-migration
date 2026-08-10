package com.migration.manatal.controller.batch;

import com.migration.manatal.service.batch.BatchService;
import com.migration.manatal.service.batch.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;
    private final ReportService reportService;

    @PostMapping("/start/{type}")
    public ResponseEntity<Map<String, Object>> startJob(@PathVariable String type) {
        var executionId = batchService.startJob(type);
        return ResponseEntity.ok(Map.of(
                "executionId", executionId,
                "status", "STARTED"
        ));
    }

    @GetMapping("/executions/{executionId}")
    public ResponseEntity<Map<String, Object>> getExecution(@PathVariable Long executionId) {
        var summary = batchService.getSummary(executionId);
        return ResponseEntity.ok(Map.of(
                "executionId", executionId,
                "summary", summary
        ));
    }

    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> getReport() {
        return ResponseEntity.ok(reportService.generateSummary());
    }

    @GetMapping("/errors")
    public ResponseEntity<List<Map<String, Object>>> getErrors(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(reportService.listErrors(limit));
    }
}
