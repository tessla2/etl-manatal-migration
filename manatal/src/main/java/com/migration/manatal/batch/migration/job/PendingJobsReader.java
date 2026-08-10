package com.migration.manatal.batch.migration.job;

import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.repository.job.JobMigrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class PendingJobsReader implements ItemReader<JobMigration> {

    private final JobMigrationRepository repository;

    private List<JobMigration> items;
    private int index;

    @Override
    public JobMigration read() {
        if (items == null) {
            items = repository.findByStatus("PENDENTE");
            index = 0;
            log.info("Migrate step: {} job(s) with status PENDENTE to process", items.size());
        }
        if (index < items.size()) {
            return items.get(index++);
        }
        return null;
    }
}
