package com.migration.manatal.batch.migration.job;


import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import com.migration.manatal.service.job.ManatalSourceJobService;
import com.migration.manatal.transform.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class JobMigrationProcessor implements ItemProcessor<JobMigration, JobMigrationPackage> {

    private final ManatalSourceJobService sourceJobService;
    private final JobMapper mapper;
    private final ClientMigrationRepository clientMigrationRepository;

    public JobMigrationPackage process(JobMigration item) {
        if (!"PENDENTE".equals(item.getStatus())) {
            return null;
        }
        log.info("Processing job {} ({})...", item.getSourceJobId(), item.getPositionName());
        JobMigrationPackage pkg = new JobMigrationPackage();
        pkg.setEntity(item);
        try {
            JobTarget target = sourceJobService.previewJobMigrated(item.getSourceJobId());
            log.info("Job {}: preview built (position='{}', organization={}, notes={})",
                    item.getSourceJobId(), target.getPositionName(), target.getOrganization(),
                    target.getNotes() == null ? 0 : target.getNotes().size());
            resolveOrganization(item, target);
            pkg.setTransformed(target);
            return pkg;
        } catch (RateLimitException e) {
            log.warn("Job {}: rate limited (429), leaving PENDENTE for retry", item.getSourceJobId());
            throw e;
        } catch (ApiException e) {
            if (e.isRetryable()) {
                log.warn("Job {}: retryable API error ({}), leaving PENDENTE for retry: {}",
                        item.getSourceJobId(), e.getStatus(), e.getMessage());
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

    private void resolveOrganization(JobMigration item, JobTarget target) {
        Integer sourceOrgId = target.getOrganization();
        if (sourceOrgId == null) {
            log.warn("Job {} has no organization in source", item.getSourceJobId());
            return;
        }

        item.setSourceOrganizationId(String.valueOf(sourceOrgId));
        Optional<ClientMigration> client = clientMigrationRepository.findBySourceOrganizationId(String.valueOf(sourceOrgId));
        if (client.isEmpty() || client.get().getTargetOrganizationId() == null) {
            log.warn("Job {}: source organization {} not found/migrated in client_migration; organization left empty in target",
                    item.getSourceJobId(), sourceOrgId);
            target.setOrganization(null);
            return;
        }

        Long targetOrgId = client.get().getTargetOrganizationId();
        item.setTargetOrganizationId(targetOrgId);
        target.setOrganization(targetOrgId.intValue());
        log.info("Job {}: source organization {} resolved to target organization {}", item.getSourceJobId(),
                sourceOrgId, targetOrgId);
    }

}
