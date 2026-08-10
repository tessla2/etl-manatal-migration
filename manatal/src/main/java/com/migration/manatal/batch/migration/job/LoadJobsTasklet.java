package com.migration.manatal.batch.migration.job;

import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.repository.job.JobMigrationRepository;
import com.migration.manatal.service.job.ManatalSourceJobService;
import com.migration.manatal.service.job.ManatalSourceJobService.JobExportInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoadJobsTasklet implements Tasklet {

    private final ManatalSourceJobService sourceService;
    private final JobMigrationRepository repository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Step: loading jobs with custom_fields.exported = 'To Export'...");

        List<JobExportInfo> jobs = sourceService.listJobWithExportedFilter();
        log.info("Load step: fetched {} job(s) with exported = 'To Export'", jobs.size());

        int totalLoaded = 0;
        int totalSkipped = 0;
        for (JobExportInfo job : jobs) {
            if (repository.findBySourceJobId(job.id()).isEmpty()) {
                JobMigration entity = new JobMigration();
                entity.setSourceJobId(job.id());
                entity.setPositionName(job.positionName());
                entity.setStatus("PENDENTE");
                repository.save(entity);
                totalLoaded++;
                log.info("Loaded job {} ({}) as PENDENTE", job.id(), job.positionName());
            } else {
                totalSkipped++;
                log.info("Skipped job {} ({}) - already exists in job_migration", job.id(), job.positionName());
            }
        }

        log.info("Load step complete. Total loaded: {}, skipped (already exists): {}", totalLoaded, totalSkipped);
        return RepeatStatus.FINISHED;
    }
}

