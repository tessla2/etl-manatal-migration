package com.migration.manatal.controller.batch;

import com.migration.manatal.service.batch.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

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
}
