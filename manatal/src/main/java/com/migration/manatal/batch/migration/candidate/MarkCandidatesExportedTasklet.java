package com.migration.manatal.batch.migration.candidate;

import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.repository.candidate.CandidateMigrationRepository;
import com.migration.manatal.service.candidate.ManatalSourceCandidateService;
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
public class MarkCandidatesExportedTasklet implements Tasklet {

    private final CandidateMigrationRepository repository;
    private final ManatalSourceCandidateService sourceService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("Step: marking successful candidates as exported in source...");

        List<CandidateMigration> candidates = repository.findByStatus("SUCESSO").stream()
                .filter(c -> !Boolean.TRUE.equals(c.getTaggedInSource()))
                .toList();

        if (candidates.isEmpty()) {
            log.info("No candidates to mark as exported");
            return RepeatStatus.FINISHED;
        }

        log.info("Found {} candidates to mark as exported", candidates.size());

        for (CandidateMigration candidate : candidates) {
            String candidateId = candidate.getSourceCandidateId();
            boolean failed = false;

            try {
                sourceService.addCandidateTag(candidateId, EXPORTED_TAG);
                log.info("Candidate {} tagged '{}'", candidateId, EXPORTED_TAG);
            } catch (Exception e) {
                failed = true;
                log.error("Candidate {}: failed to add tag '{}': {}", candidateId, EXPORTED_TAG, e.getMessage());
            }

            try {
                Long toExportTagId = sourceService.getCandidateTagId(candidateId, TO_EXPORT_TAG);
                if (toExportTagId != null) {
                    sourceService.removeCandidateTag(candidateId, toExportTagId);
                    log.info("Candidate {} tag '{}' removed", candidateId, TO_EXPORT_TAG);
                } else {
                    log.info("Candidate {} has no tag '{}' to remove", candidateId, TO_EXPORT_TAG);
                }
            } catch (Exception e) {
                failed = true;
                log.error("Candidate {}: failed to remove tag '{}': {}", candidateId, TO_EXPORT_TAG, e.getMessage());
            }

            try {
                sourceService.updateCustomField(candidateId, "exported", "Yes");
                log.info("Candidate {} custom field exported=Yes set", candidateId);
            } catch (Exception e) {
                failed = true;
                log.error("Candidate {}: failed to set exported=Yes: {}", candidateId, e.getMessage());
            }

            if (!failed) {
                candidate.setTaggedInSource(true);
                repository.save(candidate);
            }
        }

        return RepeatStatus.FINISHED;
    }

    private static final String EXPORTED_TAG = "Exported";
    private static final String TO_EXPORT_TAG = "To Export";
}
