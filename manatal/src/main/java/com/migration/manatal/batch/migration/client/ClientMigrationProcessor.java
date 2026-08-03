package com.migration.manatal.batch.migration.client;

import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.service.client.ManatalSourceClientService;
import com.migration.manatal.transform.ClientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientMigrationProcessor implements ItemProcessor<ClientMigration, ClientMigrationPackage> {

    private final ManatalSourceClientService sourceClientService;
    private final ClientMapper clientMapper;

    public ClientMigrationPackage process(ClientMigration item) {
        if (!"PENDENTE".equals(item.getStatus())) {
            return null;
        }
        ClientMigrationPackage pkg = new ClientMigrationPackage();
        pkg.setEntity(item);
        try {
            ClientTarget target = sourceClientService.previewClientMigrated(item.getSourceOrganizationId());
            pkg.setTransformed(target);
            return pkg;
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            if (e.isRetryable()) {
                throw e;
            }
            log.error("Error processing client {}: {}", item.getSourceOrganizationId(), e.getMessage());
            pkg.setErrorMessage(e.getMessage());
            return pkg;
        } catch (Exception e) {
            log.error("Error processing client {}: {}", item.getSourceOrganizationId(), e.getMessage());
            pkg.setErrorMessage(e.getMessage());
            return pkg;
        }
    }
}
