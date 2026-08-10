package com.migration.manatal.controller.client;

import com.migration.manatal.model.client.ClientSource;
import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.service.client.ManatalSourceClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company-source")
@RequiredArgsConstructor
public class CompanySourceClientController {

    private final ManatalSourceClientService service;

    @GetMapping("/organizations")
    public ResponseEntity<String> listOrganizations(
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(service.listOrganizations(offset));
    }

    @GetMapping("/contacts")
    public ResponseEntity<String> listContacts(
            @RequestParam(defaultValue = "0") int offset) {
        return ResponseEntity.ok(service.listContacts(offset));
    }

    @GetMapping("/organizations/{id}")
    public ResponseEntity<String> getOrganizationById(@PathVariable String id){
        return ResponseEntity.ok(service.fetchOrganizationById(id));
    }


    @GetMapping("/organizations/{id}/preview")
    public ResponseEntity<ClientTarget> previewOrganization(@PathVariable String id) {
        return ResponseEntity.ok(service.previewClientMigrated(id));
    }

    @GetMapping("/organizations/{id}/notes")
    public ResponseEntity<String> listNotes(@PathVariable String id) {
        return ResponseEntity.ok(service.listOrganizationNotes(id, 0));
    }

}
