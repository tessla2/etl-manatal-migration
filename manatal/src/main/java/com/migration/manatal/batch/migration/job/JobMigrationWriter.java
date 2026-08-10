package com.migration.manatal.batch.migration.job;

import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.repository.job.JobMigrationRepository;
import com.migration.manatal.service.job.ManatalSourceJobService;
import com.migration.manatal.service.job.ManatalTargetJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobMigrationWriter implements ItemWriter<JobMigrationPackage> {

    private final ManatalTargetJobService targetService;
    private final ManatalSourceJobService sourceService;
    private final JobMigrationRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void write(Chunk<? extends JobMigrationPackage> chunk) throws Exception {
        for (JobMigrationPackage pkg : chunk) {
            JobMigration entity = pkg.getEntity();

            if (entity.getTargetJobId() != null) {
                log.warn("Job {} already exists in DB with target job id {} — skipping",
                        entity.getSourceJobId(), entity.getTargetJobId());
                continue;
            }

            try {
                if (pkg.getErrorMessage() != null) {
                    log.info("Job {}: skipping write - error from processing", entity.getSourceJobId());
                    markErro(entity, pkg.getErrorMessage());
                    continue;
                }

                log.info("Job {} ({}): writing to target...", entity.getSourceJobId(), entity.getPositionName());
                JobTarget transformed = pkg.getTransformed();
                List<JobTarget.TargetNote> notes = transformed.getNotes() == null ? List.of() : transformed.getNotes();
                transformed.setNotes(null);

                String response = targetService.migrateJob(transformed);
                long targetJobId;
                try {
                    targetJobId = parseTargetJobId(response);
                } catch (Exception e) {
                    markErro(entity, "Job created but failed to parse target id: " + e.getMessage());
                    continue;
                }

                entity.setTargetJobId(targetJobId);
                entity.setStatus("SUCESSO");
                entity.setErrorMessage(null);
                repository.save(entity);
                log.info("Job {}: created target job {} (SUCESSO)", entity.getSourceJobId(), targetJobId);

                try {
                    sourceService.updateCustomField(entity.getSourceJobId(), "exported", "Yes");
                    entity.setTaggedInSource(true);
                    repository.save(entity);
                    log.info("Job {} migrated successfully and marked as exported", entity.getSourceJobId());
                } catch (Exception e) {
                    log.warn("Job {} migrated but failed to mark exported: {}", entity.getSourceJobId(), e.getMessage());
                }

                postNotes(targetJobId, notes, entity);
            } catch (RateLimitException e) {
                log.warn("Job {}: rate limited (429) during write, leaving PENDENTE for retry", entity.getSourceJobId());
                throw e;
            } catch (ApiException e) {
                if (e.isRetryable()) {
                    log.warn("Job {}: retryable API error ({}) during write, leaving PENDENTE for retry: {}",
                            entity.getSourceJobId(), e.getStatus(), e.getMessage());
                    throw e;
                }
                markErro(entity, e.getMessage());
            } catch (Exception e) {
                markErro(entity, e.getMessage());
            }
        }

    }

    private void postNotes(long targetJobId, List<JobTarget.TargetNote> notes, JobMigration entity) {
        if (notes.isEmpty()) {
            log.info("Job {} has no notes to post", entity.getSourceJobId());
            return;
        }
        int posted = 0;
        for (JobTarget.TargetNote note : notes) {
            if (note.getContent() == null || note.getContent().isBlank()) {
                continue;
            }
            try {
                targetService.createJobNote((int) targetJobId, note.getContent());
                posted++;
            } catch (Exception e) {
                log.warn("Job {}: failed to post note '{}': {}", entity.getSourceJobId(),
                        preview(note.getContent()), e.getMessage());
            }
        }
        log.info("Job {}: posted {}/{} notes to target job {}", entity.getSourceJobId(), posted,
                notes.size(), targetJobId);
    }

    private long parseTargetJobId(String response) {
        return objectMapper.readTree(response).path("id").asLong();
    }

    private String preview(String content) {
        if (content.length() <= 60) return content;
        return content.substring(0, 60) + "...";
    }

    private void markErro(JobMigration entity, String message) {
        entity.setStatus("ERRO");
        entity.setErrorMessage(message);
        repository.save(entity);
        log.error("Job {} marked as ERRO due to: {}", entity.getSourceJobId(), message);
    }
}
