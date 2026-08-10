package com.migration.manatal.controller.candidate;


import com.migration.manatal.service.candidate.ManatalTargetCandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/company-target")
@RestController
@RequiredArgsConstructor
public class CompanyTargetCandidateController {

    private final ManatalTargetCandidateService service;

    @GetMapping("/candidates/{id}")
    public ResponseEntity<String> getCandidateById(@PathVariable String id) {
        return ResponseEntity.ok(service.fetchCandidateById(id));
    }
}
