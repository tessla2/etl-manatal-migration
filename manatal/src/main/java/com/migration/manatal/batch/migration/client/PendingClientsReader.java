package com.migration.manatal.batch.migration.client;

import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
public class PendingClientsReader implements ItemReader<ClientMigration> {

    private final ClientMigrationRepository repository;

    private List<ClientMigration> items;
    private int index;

    @Override
    public ClientMigration read() {
        if (items == null) {
            items = repository.findByStatus("PENDENTE");
            index = 0;
        }
        if (index < items.size()) {
            return items.get(index++);
        }
        return null;
    }
}
