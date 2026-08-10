package com.migration.manatal.batch.migration.candidate;

import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.repository.candidate.CandidateMigrationRepository;
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
public class PendingCandidatesReader implements ItemReader<CandidateMigration> {

    private final CandidateMigrationRepository repository;

    private List<CandidateMigration> items;
    private int index;

    @Override
    public CandidateMigration read() {
        if (items == null) {
            items = repository.findByStatus("PENDENTE");
            index = 0;
            log.info("Candidate migration step: {} PENDENTE candidate(s) picked up", items.size());
        }
        if (index < items.size()) {
            return items.get(index++);
        }
        return null;
    }
}
