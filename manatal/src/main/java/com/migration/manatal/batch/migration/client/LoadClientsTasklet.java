package com.migration.manatal.batch.migration.client;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import com.migration.manatal.service.client.ManatalSourceClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoadClientsTasklet implements Tasklet {

    private final ManatalSourceClientService sourceService;
    private final ClientMigrationRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Step: loading clients with custom_fields.exported = 'To Export'...");

        int offset = 0;
        int pageSize = 100;
        int totalLoaded = 0;

        while (true) {
            String json = sourceService.listOrganizationsWithExportFilter(offset);
            JsonNode root = objectMapper.readTree(json);
            JsonNode results = root.path("results");

            if (!results.isArray() || results.isEmpty()) {
                log.info("No more organizations found at offset {}", offset);
                break;
            }

            for (JsonNode org : results) {
                String id = org.path("id").asText();
                String name = org.path("name").asText("");

                if (repository.findBySourceOrganizationId(id).isEmpty()) {
                    ClientMigration entity = new ClientMigration();
                    entity.setSourceOrganizationId(id);
                    entity.setSourceName(name);
                    entity.setStatus("PENDENTE");
                    repository.save(entity);
                    totalLoaded++;
                    log.info("Loaded client {} ({}) as PENDENTE", id, name);
                }
            }

            int count = results.size();
            if (count < pageSize) break;
            offset += pageSize;
        }

        log.info("Load step complete. Total clients loaded: {}", totalLoaded);
        return RepeatStatus.FINISHED;
    }
}
