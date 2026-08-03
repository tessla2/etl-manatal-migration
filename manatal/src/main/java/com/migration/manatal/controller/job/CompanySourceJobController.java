package com.migration.manatal.controller.job;

import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.service.job.ManatalSourceJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company-source-job")
@RequiredArgsConstructor
public class CompanySourceJobController {

    private final ManatalSourceJobService service;

    @GetMapping("/jobs/{id}")
    public ResponseEntity<String> getJobById(@PathVariable String id) {
        return ResponseEntity.ok(service.getJobById(id));
    }

    @GetMapping("/jobs/{id}/preview")
    public ResponseEntity<JobTarget> previewJob(@PathVariable String id) {
        return ResponseEntity.ok(service.previewJobMigrated(id));
    }

    @GetMapping("/jobs/with-contacts")
    public ResponseEntity<java.util.List<ManatalSourceJobService.JobContactInfo>> listJobsWithContacts() {
        return ResponseEntity.ok(service.listJobsWithContacts());
    }

}
