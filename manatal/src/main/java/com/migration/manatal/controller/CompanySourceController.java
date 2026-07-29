package com.migration.manatal.controller;


import com.migration.manatal.service.ManatalSourceClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/company-one")
@RequiredArgsConstructor
public class CompanySourceController {

    private final ManatalSourceClientService service;

    @GetMapping("/organizations")
    public ResponseEntity<String> listOrganizations(
            @RequestParam(defaultValue = "0") int offset) {
        try {
            return ResponseEntity.ok(service.listOrganizations(offset));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Erro ao consultar Manatal: " + e.getMessage());
        }
    }

    @GetMapping("/contacts")
    public ResponseEntity<String> listContacts(
            @RequestParam(defaultValue = "0") int offset) {
        try {
            return ResponseEntity.ok(service.listContacts(offset));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Erro ao consultar Manatal: " + e.getMessage());
        }
    }

}
