package com.migration.manatal.controller.client;

import com.migration.manatal.service.client.ManatalSourceClientService;
import com.migration.manatal.service.client.ManatalTargetClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company-target")
@RequiredArgsConstructor
public class CompanyTargetClientController {

    private final ManatalSourceClientService sourceService;
    private final ManatalTargetClientService targetService;

    @PostMapping("/migrate/{organizationId}")
    public ResponseEntity<String> migrateOrganization(@PathVariable String organizationId) {
        var preview = sourceService.previewClientMigrated(organizationId);
        var result = targetService.migrateOrganization(preview);
        return ResponseEntity.ok(result);
    }
}
