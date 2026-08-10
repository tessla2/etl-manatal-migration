package com.migration.manatal.controller.job;

import com.migration.manatal.service.job.ManatalTargetJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/company-target")
@RequiredArgsConstructor
public class CompanyTargetJobController {

    private final ManatalTargetJobService service;
    private final ObjectMapper objectMapper;

    @GetMapping("/jobs/{id}")
    public ResponseEntity<String> getJobById(@PathVariable String id) {
        return ResponseEntity.ok(service.getJobById(id));
    }

    @GetMapping("/job-pipelines/{id}/stages")
    public ResponseEntity<List<Map<String, Object>>> getPipelineStages(@PathVariable String id) throws Exception {
        String pipelineJson = service.getJobPipeline(Integer.parseInt(id));
        var stages = objectMapper.readTree(pipelineJson).path("job_pipeline_stages");
        List<Map<String, Object>> result = new ArrayList<>();
        if (stages.isArray()) {
            for (var stage : stages) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", stage.path("id").asInt());
                entry.put("name", stage.path("name").asString());
                entry.put("rank", stage.path("rank").asInt());
                result.add(entry);
            }
        }
        return ResponseEntity.ok(result);
    }

}
