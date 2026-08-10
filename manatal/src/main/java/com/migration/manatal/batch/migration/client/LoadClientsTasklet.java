package com.migration.manatal.batch.migration.client;

import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import com.migration.manatal.service.client.ManatalSourceClientService;
import com.migration.manatal.service.client.ManatalSourceClientService.OrganizationExportInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoadClientsTasklet implements Tasklet {

    private final ManatalSourceClientService sourceService;
    private final ClientMigrationRepository repository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Step: loading clients with custom_fields.exported = 'To Export'...");

        List<OrganizationExportInfo> organizations = sourceService.listOrganizationsWithExportFilter();
        log.info("Step: fetched {} organization(s) marked 'To Export' from source", organizations.size());

        int totalLoaded = 0;
        int totalSkipped = 0;
        for (OrganizationExportInfo org : organizations) {
            if (repository.findBySourceOrganizationId(org.id()).isEmpty()) {
                ClientMigration entity = new ClientMigration();
                entity.setSourceOrganizationId(org.id());
                entity.setSourceName(org.name());
                entity.setStatus("PENDENTE");
                repository.save(entity);
                totalLoaded++;
                log.info("Loaded client {} ({}) as PENDENTE", org.id(), org.name());
            } else {
                totalSkipped++;
                log.info("Client {} ({}) already exists, skipping", org.id(), org.name());
            }
        }

        log.info("Load step complete. Total clients loaded: {} ({} already existing, skipped)", totalLoaded, totalSkipped);
        return RepeatStatus.FINISHED;
    }
}
