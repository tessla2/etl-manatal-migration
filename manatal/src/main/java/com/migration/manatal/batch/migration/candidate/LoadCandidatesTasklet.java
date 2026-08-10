package com.migration.manatal.batch.migration.candidate;

import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.repository.candidate.CandidateMigrationRepository;
import com.migration.manatal.service.candidate.ManatalSourceCandidateService;
import com.migration.manatal.service.candidate.ManatalSourceCandidateService.CandidateExportInfo;
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
public class LoadCandidatesTasklet implements Tasklet {

    private final ManatalSourceCandidateService sourceService;
    private final CandidateMigrationRepository repository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Step: loading candidates with tag 'To Export'...");

        List<CandidateExportInfo> candidates = sourceService.listCandidatesWithExportFilter();

        int totalLoaded = 0;
        int totalSkipped = 0;
        for (CandidateExportInfo candidate : candidates) {
            if (repository.findBySourceCandidateId(candidate.id()).isEmpty()) {
                CandidateMigration entity = new CandidateMigration();
                entity.setSourceCandidateId(candidate.id());
                entity.setFullName(candidate.fullName());
                entity.setStatus("PENDENTE");
                repository.save(entity);
                totalLoaded++;
                log.info("Loaded candidate {} ({}) as PENDENTE", candidate.id(), candidate.fullName());
            } else {
                totalSkipped++;
                log.info("Candidate {} ({}) already exists, skipping", candidate.id(), candidate.fullName());
            }
        }

        log.info("Load step complete. Total candidates loaded: {} ({} already existing, skipped)", totalLoaded, totalSkipped);
        return RepeatStatus.FINISHED;
    }
}
