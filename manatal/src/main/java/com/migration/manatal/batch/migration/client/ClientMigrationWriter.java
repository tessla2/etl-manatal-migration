package com.migration.manatal.batch.migration.client;

import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import com.migration.manatal.service.client.ManatalSourceClientService;
import com.migration.manatal.service.client.ManatalTargetClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientMigrationWriter implements ItemWriter<ClientMigrationPackage> {

    private final ManatalTargetClientService targetService;
    private final ManatalSourceClientService sourceService;
    private final ClientMigrationRepository repository;

    @Override
    public void write(Chunk<? extends ClientMigrationPackage> chunk) {
        for (ClientMigrationPackage pkg : chunk) {
            ClientMigration entity = pkg.getEntity();

            try {
                if (pkg.getErrorMessage() != null) {
                    entity.setStatus("ERRO");
                    entity.setErrorMessage(pkg.getErrorMessage());
                    repository.save(entity);
                    log.error("Client {} marked as ERRO due to: {}", entity.getSourceOrganizationId(), pkg.getErrorMessage());
                    continue;
                }

                targetService.migrateOrganization(pkg.getTransformed());
                entity.setStatus("SUCESSO");
                repository.save(entity);

                sourceService.updateCustomField(entity.getSourceOrganizationId(), "exported", "Yes");
                entity.setTaggedInSource(true);
                repository.save(entity);

                log.info("Client {} migrated successfully and marked as exported", entity.getSourceOrganizationId());
            } catch (Exception e) {
                log.error("Error writing client {}: {}", entity.getSourceOrganizationId(), e.getMessage());
                entity.setStatus("ERRO");
                entity.setErrorMessage(e.getMessage());
                repository.save(entity);
            }
        }
    }

}
