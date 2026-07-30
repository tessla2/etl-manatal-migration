package com.migration.manatal.service.batch;

import com.migration.manatal.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final JobOperator jobOperator;

    public Long startJob(String type) {
        var jobName = switch (type.toUpperCase()) {
            case "CLIENT" -> "clientMigrationJob";
            case "CANDIDATE" -> "candidateMigrationJob";
            case "JOB" -> "jobMigrationJob";
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown job type: " + type);
        };

        var params = new Properties();
        params.setProperty("timestamp", LocalDateTime.now().toString());

        try {
            return jobOperator.start(jobName, params);
        } catch (NoSuchJobException e) {
            throw ApiException.notFound("Job not found: " + jobName);
        } catch (JobInstanceAlreadyExistsException | InvalidJobParametersException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Job already running: " + e.getMessage());
        }
    }

    public String getSummary(Long executionId) {
        try {
            return jobOperator.getSummary(executionId);
        } catch (NoSuchJobExecutionException e) {
            throw ApiException.notFound("Execution not found: " + executionId);
        }
    }
}
