package com.migration.manatal.batch.migration.job;

import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.repository.job.JobMigrationRepository;
import com.migration.manatal.service.job.ManatalSourceJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoadJobsTasklet implements Tasklet {

    private final ManatalSourceJobService sourceService;
    private final JobMigrationRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Step: loading jobs with custom_fields.Exported = 'To Export'...");

        int offset = 0;
        int pageSize = 100;
        int totalLoaded = 0;

        while (true) {
            String json = sourceService.listJobWithExportedFilter(offset);
            JsonNode root = objectMapper.readTree(json);
            JsonNode results = root.path("results");

            if (!results.isArray() || results.isEmpty()) {
                log.info("No more jobs found at offset {}", offset);
                break;
            }

            for (JsonNode job : results) {
                String id = job.path("id").asString();
                String name = job.path("position_name").asString("");

                if (repository.findBySourceJobId(id).isEmpty()) {
                    JobMigration entity = new JobMigration();
                    entity.setSourceJobId(id);
                    entity.setPositionName(name);
                    entity.setStatus("PENDENTE");
                    repository.save(entity);
                    totalLoaded++;
                    log.info("Loaded job {} ({}) as PENDENTE", id, name);
                }

            }
            int count = results.size();
            if (count < pageSize) break;
            offset += pageSize;
        }

        log.info("Load step complete. Total jobs loaded: {}", totalLoaded);
        return RepeatStatus.FINISHED;
    }

}

