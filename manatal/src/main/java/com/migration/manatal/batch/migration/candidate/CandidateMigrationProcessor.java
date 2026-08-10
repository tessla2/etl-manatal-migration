package com.migration.manatal.batch.migration.candidate;


import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.candidate.CandidateTarget;
import com.migration.manatal.service.candidate.ManatalSourceCandidateService;
import com.migration.manatal.transform.CandidateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateMigrationProcessor implements ItemProcessor<CandidateMigration, CandidateMigrationPackage> {

    private final ManatalSourceCandidateService sourceCandidateService;
    private final CandidateMapper mapper;

    public CandidateMigrationPackage process(CandidateMigration item) {
        if (!"PENDENTE".equals(item.getStatus())) {
            return null;
        }
        log.info("Processing candidate {} ({})...", item.getSourceCandidateId(), item.getFullName());
        CandidateMigrationPackage pkg = new CandidateMigrationPackage();
        pkg.setEntity(item);
        try {
            CandidateTarget target = sourceCandidateService.previewCandidateMigrated(item.getSourceCandidateId());
            log.info("Candidate {}: preview built (name='{}', email='{}', location='{}', notes={}, nationalities={}, skills={})",
                    item.getSourceCandidateId(), target.getFullName(), target.getEmail(),
                    target.getCandidateLocation(),
                    target.getNotes() == null ? 0 : target.getNotes().size(),
                    target.getNationalities() == null ? 0 : target.getNationalities().size(),
                    target.getSkills() == null ? 0 : target.getSkills().size());
            pkg.setTransformed(target);
            return pkg;
        } catch (RateLimitException e) {
            log.warn("Candidate {}: rate limited (429), leaving PENDENTE for retry", item.getSourceCandidateId());
            throw e;
        } catch (ApiException e) {
            if (e.isRetryable()) {
                log.warn("Candidate {}: retryable API error ({}), leaving PENDENTE for retry: {}",
                        item.getSourceCandidateId(), e.getStatus(), e.getMessage());
                throw e;
            }
            log.error("Error processing candidate {}: {}", item.getSourceCandidateId(), e.getMessage());
            pkg.setErrorMessage(e.getMessage());
            return pkg;
        } catch (Exception e) {
            log.error("Error processing candidate {}: {}", item.getSourceCandidateId(), e.getMessage());
            pkg.setErrorMessage(e.getMessage());
            return pkg;
        }
    }


}
