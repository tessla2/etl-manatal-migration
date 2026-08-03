package com.migration.manatal.batch.migration.job;


import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.service.job.ManatalSourceJobService;
import com.migration.manatal.transform.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class JobMigrationProcessor implements ItemProcessor<JobMigration, JobMigrationPackage> {

    private final ManatalSourceJobService sourceJobService;
    private final JobMapper mapper;

    public JobMigrationPackage process(JobMigration item) {
        if (!"PENDENTE".equals(item.getStatus())) {
            return null;
        }
        JobMigrationPackage pkg = new JobMigrationPackage();
        pkg.setEntity(item);
        try {
            JobTarget target = sourceJobService.previewJobMigrated(item.getSourceJobId());
            pkg.setTransformed(target);
            return pkg;
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            if (e.isRetryable()) {
                throw e;
            }
            log.error("Error processing job {}: {}", item.getSourceJobId(), e.getMessage());
            pkg.setErrorMessage(e.getMessage());
            return pkg;
        } catch (Exception e) {
            log.error("Error processing job {}: {}", item.getSourceJobId(), e.getMessage());
            pkg.setErrorMessage(e.getMessage());
            return pkg;
        }
    }


}
