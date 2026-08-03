package com.migration.manatal.controller.job;

import com.migration.manatal.service.job.ManatalSourceJobService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanySourceControllerTest {

    @Mock
    private ManatalSourceJobService service;

    @InjectMocks
    private CompanySourceJobController controller;

    @Test
    void shouldReturnJobJsonForGivenId() {
        String jobJson = "{\"id\": 42, \"position_name\": \"Dev\"}";
        when(service.getJobById("42")).thenReturn(jobJson);

        ResponseEntity<String> response = controller.getJobById("42");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(jobJson, response.getBody());
        verify(service).getJobById("42");
    }

    @Test
    void shouldReturnPreviewForGivenId() {
        var target = new com.migration.manatal.model.job.JobTarget();
        target.setPositionName("Dev");
        when(service.previewJobMigrated("42")).thenReturn(target);

        var response = controller.previewJob("42");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(target, response.getBody());
        verify(service).previewJobMigrated("42");
    }

    @Test
    void shouldReturnJobsWithContacts() {
        var expected = java.util.List.of(
                new ManatalSourceJobService.JobContactInfo(1, "Dev", "4796484"));
        when(service.listJobsWithContacts()).thenReturn(expected);

        var response = controller.listJobsWithContacts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(expected, response.getBody());
        verify(service).listJobsWithContacts();
    }
}
