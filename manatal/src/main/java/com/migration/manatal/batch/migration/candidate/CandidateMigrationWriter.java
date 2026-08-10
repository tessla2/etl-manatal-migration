package com.migration.manatal.batch.migration.candidate;

import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.candidate.CandidateSource;
import com.migration.manatal.model.candidate.CandidateTarget;
import com.migration.manatal.repository.candidate.CandidateMigrationRepository;
import com.migration.manatal.repository.job.JobMigrationRepository;
import com.migration.manatal.service.candidate.ManatalSourceCandidateService;
import com.migration.manatal.service.candidate.ManatalTargetCandidateService;
import com.migration.manatal.transform.StageNameMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CandidateMigrationWriter implements ItemWriter<CandidateMigrationPackage> {

    private final ManatalTargetCandidateService targetService;
    private final ManatalSourceCandidateService sourceService;
    private final JobMigrationRepository jobRepository;
    private final CandidateMigrationRepository repository;
    private final ObjectMapper objectMapper;
    private final StageNameMapper stageNameMapper;

    @Override
    public void write(Chunk<? extends CandidateMigrationPackage> chunk) throws Exception {
        for (CandidateMigrationPackage pkg : chunk) {
            CandidateMigration entity = pkg.getEntity();

            if (entity.getTargetCandidateId() != null) {
                log.warn("Candidate {} already exists in DB with target candidate id {} — skipping",
                        entity.getSourceCandidateId(), entity.getTargetCandidateId());
                continue;
            }

            try {
                if (pkg.getErrorMessage() != null) {
                    markErro(entity, pkg.getErrorMessage());
                    continue;
                }

                log.info("Writing candidate {} ({})...", entity.getSourceCandidateId(), entity.getFullName());
                CandidateTarget transformed = pkg.getTransformed();
                List<CandidateTarget.TargetNote> notes =
                        transformed.getNotes() == null ? List.of() : transformed.getNotes();
                List<CandidateTarget.TargetNationality> nationalities =
                        transformed.getNationalities() == null ? List.of() : transformed.getNationalities();
                List<CandidateTarget.TargetSkill> skills =
                        transformed.getSkills() == null ? List.of() : transformed.getSkills();
                transformed.setNotes(null);
                transformed.setNationalities(null);
                transformed.setSkills(null);

                String response = targetService.migrateCandidate(transformed);
                long targetCandidateId;
                try {
                    targetCandidateId = parseTargetCandidateId(response);
                } catch (Exception e) {
                    markErro(entity, "Candidate created but failed to parse target id: " + e.getMessage());
                    continue;
                }

                entity.setTargetCandidateId(targetCandidateId);
                entity.setStatus("SUCESSO");
                entity.setErrorMessage(null);
                repository.save(entity);

                postNotes(entity, (int) targetCandidateId, notes);
                postNationalities(entity, (int) targetCandidateId, nationalities);
                postSkills(entity, (int) targetCandidateId, skills);
                migrateMatches(entity, (int) targetCandidateId);
                migrateSocialMedia(entity, (int) targetCandidateId);
                migrateActivities(entity, (int) targetCandidateId);
                migrateResume(entity, (int) targetCandidateId);
                migrateAttachments(entity, (int) targetCandidateId);
            } catch (RateLimitException e) {
                throw e;
            } catch (ApiException e) {
                if (e.isRetryable()) {
                    throw e;
                }
                markErro(entity, e.getMessage());
            } catch (Exception e) {
                markErro(entity, e.getMessage());
            }
        }
    }

    private void postNotes(CandidateMigration entity, int targetCandidateId, List<CandidateTarget.TargetNote> notes) {
        if (notes.isEmpty()) {
            log.info("Candidate {} has no notes to post", entity.getSourceCandidateId());
            return;
        }
        int posted = 0;
        for (CandidateTarget.TargetNote note : notes) {
            if (note.getContent() == null || note.getContent().isBlank()) {
                continue;
            }
            try {
                targetService.createCandidateNote(targetCandidateId, note.getContent());
                posted++;
            } catch (Exception e) {
                log.warn("Candidate {}: failed to post note '{}': {}", entity.getSourceCandidateId(),
                        preview(note.getContent()), e.getMessage());
            }
        }
        log.info("Candidate {}: posted {}/{} notes to target candidate {}", entity.getSourceCandidateId(), posted,
                notes.size(), targetCandidateId);
    }

    private void postNationalities(CandidateMigration entity, int targetCandidateId,
                                   List<CandidateTarget.TargetNationality> nationalities) {
        if (nationalities.isEmpty()) {
            return;
        }
        int posted = 0;
        for (CandidateTarget.TargetNationality nationality : nationalities) {
            if (nationality.getCountry() == null || nationality.getCountry().isBlank()) {
                continue;
            }
            try {
                targetService.createCandidateNationality(targetCandidateId, nationality.getCountry());
                posted++;
            } catch (Exception e) {
                log.warn("Candidate {}: failed to post nationality '{}': {}", entity.getSourceCandidateId(),
                        nationality.getCountry(), e.getMessage());
            }
        }
        log.info("Candidate {}: posted {}/{} nationalities to target candidate {}", entity.getSourceCandidateId(),
                posted, nationalities.size(), targetCandidateId);
    }

    private void postSkills(CandidateMigration entity, int targetCandidateId,
                            List<CandidateTarget.TargetSkill> skills) {
        if (skills.isEmpty()) {
            log.info("Candidate {} has no skills to post", entity.getSourceCandidateId());
            return;
        }
        try {
            targetService.addCandidateSkills(targetCandidateId, skills);
            log.info("Candidate {}: posted {} skills to target candidate {}", entity.getSourceCandidateId(),
                    skills.size(), targetCandidateId);
        } catch (Exception e) {
            log.warn("Candidate {}: failed to post {} skills: {}", entity.getSourceCandidateId(), skills.size(),
                    e.getMessage());
        }
    }

    private void migrateMatches(CandidateMigration entity, int targetCandidateId) {
        List<CandidateSource.CandidateMatch> matches;
        try {
            matches = sourceService.getCandidateMatches(entity.getSourceCandidateId());
        } catch (Exception e) {
            log.warn("Candidate {}: failed to load source matches: {}", entity.getSourceCandidateId(), e.getMessage());
            return;
        }
        if (matches.isEmpty()) {
            log.info("Candidate {} has no matches to migrate", entity.getSourceCandidateId());
            return;
        }
        int migrated = 0;
        boolean stored = false;
        for (CandidateSource.CandidateMatch match : matches) {
            if (match.getJob() == null) {
                continue;
            }
            try {
                Optional<com.migration.manatal.entity.job.JobMigration> jobMig =
                        jobRepository.findBySourceJobId(String.valueOf(match.getJob()));
                if (jobMig.isEmpty() || jobMig.get().getTargetJobId() == null) {
                    log.warn("Candidate {}: match to source job {} skipped (job not migrated to target)",
                            entity.getSourceCandidateId(), match.getJob());
                    continue;
                }
                int targetJobId = jobMig.get().getTargetJobId().intValue();

                String createResp = targetService.createCandidateMatch(targetCandidateId, targetJobId);
                int matchId = parseTargetMatchId(createResp);

                String stageName = match.getStageName();
                String targetStageName = stageName == null ? null : stageNameMapper.resolve(stageName);
                if (stageName == null || stageName.isBlank()) {
                    log.info("Candidate {}: match to source job {} has no stage in source",
                            entity.getSourceCandidateId(), match.getJob());
                } else {
                    log.info("Candidate {}: source stage '{}' -> target stage '{}'",
                            entity.getSourceCandidateId(), stageName, targetStageName);
                }
                if (targetStageName != null && !targetStageName.isBlank()) {
                    Integer pipelineId = parsePipelineIdFromMatch(createResp);
                    if (pipelineId == null) {
                        log.warn("Candidate {}: cannot resolve stage '{}' for target job {} — match response has no job_pipeline; keeping default",
                                entity.getSourceCandidateId(), targetStageName, targetJobId);
                    } else {
                        Integer stageId = findPipelineStageIdByStageName(pipelineId, targetStageName);
                        if (stageId != null) {
                            targetService.updateMatchStage(matchId, stageId);
                        } else {
                            log.warn("Candidate {}: stage '{}' not found in target pipeline {} for job {}; keeping default",
                                    entity.getSourceCandidateId(), targetStageName, pipelineId, targetJobId);
                        }
                    }
                }

                if (Boolean.FALSE.equals(match.getIsActive())) {
                    targetService.dropMatch(matchId, match.getDroppedAt());
                    targetService.createMatchNote(matchId,
                            "Dropado em " + match.getDroppedAt() + " do stage " + targetStageName);
                }
                migrated++;
                if (!stored) {
                    entity.setSourceJobId(String.valueOf(match.getJob()));
                    entity.setTargetJobId((long) targetJobId);
                    entity.setStageName(stageName);
                    stored = true;
                }
            } catch (Exception e) {
                log.warn("Candidate {}: failed to migrate match to source job {}: {}",
                        entity.getSourceCandidateId(), match.getJob(), e.getMessage());
            }
        }
        if (stored) {
            repository.save(entity);
        }
        log.info("Candidate {}: migrated {}/{} matches", entity.getSourceCandidateId(), migrated, matches.size());
    }

    private void migrateSocialMedia(CandidateMigration entity, int targetCandidateId) {
        List<CandidateSource.SocialMedia> socialMedia;
        try {
            socialMedia = sourceService.getCandidateSocialMedia(entity.getSourceCandidateId());
        } catch (Exception e) {
            log.warn("Candidate {}: failed to load source social media: {}", entity.getSourceCandidateId(),
                    e.getMessage());
            return;
        }
        if (socialMedia.isEmpty()) {
            log.info("Candidate {} has no social media to migrate", entity.getSourceCandidateId());
            return;
        }
        int posted = 0;
        for (CandidateSource.SocialMedia sm : socialMedia) {
            if (sm.getSocialMedia() == null || sm.getSocialMedia().isBlank()
                    || sm.getSocialMediaUrl() == null || sm.getSocialMediaUrl().isBlank()) {
                continue;
            }
            try {
                targetService.createCandidateSocialMedia(targetCandidateId, sm.getSocialMedia(), sm.getSocialMediaUrl());
                posted++;
            } catch (Exception e) {
                log.warn("Candidate {}: failed to post social media '{}': {}", entity.getSourceCandidateId(),
                        sm.getSocialMedia(), e.getMessage());
            }
        }
        log.info("Candidate {}: posted {}/{} social media links to target candidate {}", entity.getSourceCandidateId(),
                posted, socialMedia.size(), targetCandidateId);
    }

    private void migrateActivities(CandidateMigration entity, int targetCandidateId) {
        List<CandidateSource.Activity> activities;
        try {
            activities = sourceService.getCandidateActivities(entity.getSourceCandidateId());
        } catch (Exception e) {
            log.warn("Candidate {}: failed to load source activities: {}", entity.getSourceCandidateId(),
                    e.getMessage());
            return;
        }
        if (activities.isEmpty()) {
            log.info("Candidate {} has no activities to migrate", entity.getSourceCandidateId());
            return;
        }
        Map<Integer, String> userNames;
        try {
            userNames = sourceService.listUsersBestEffort();
        } catch (Exception e) {
            userNames = Map.of();
        }
        int posted = 0;
        for (CandidateSource.Activity activity : activities) {
            String creatorName = activity.getCreator() == null ? null : userNames.get(activity.getCreator());
            String content = activityNote(activity, creatorName);
            if (content == null || content.isBlank()) {
                continue;
            }
            try {
                targetService.createCandidateNote(targetCandidateId, content);
                posted++;
            } catch (Exception e) {
                log.warn("Candidate {}: failed to post activity note '{}': {}", entity.getSourceCandidateId(),
                        preview(content), e.getMessage());
            }
        }
        log.info("Candidate {}: posted {}/{} activities as notes to target candidate {}", entity.getSourceCandidateId(),
                posted, activities.size(), targetCandidateId);
    }

    private String activityNote(CandidateSource.Activity activity, String creatorName) {
        List<String> parts = new ArrayList<>();
        if (activity.getName() != null && !activity.getName().isBlank()) {
            parts.add(activity.getName());
        }
        String dia = dateOnly(activity.getDueDate());
        if (dia != null && !dia.isBlank()) {
            parts.add(dia);
        }
        if (creatorName != null && !creatorName.isBlank()) {
            parts.add(creatorName);
        }
        String nota = activity.getDescription();
        String head = String.join(" - ", parts);
        if (nota == null || nota.isBlank()) {
            return head;
        }
        if (head.isEmpty()) {
            return nota;
        }
        return head + ": " + nota;
    }

    private String dateOnly(String datetime) {
        if (datetime == null) {
            return null;
        }
        int idx = datetime.indexOf('T');
        if (idx >= 0) {
            return datetime.substring(0, idx);
        }
        return datetime;
    }

    private void migrateResume(CandidateMigration entity, int targetCandidateId) {
        try {
            CandidateSource.Resume resume = sourceService.getCandidateResume(entity.getSourceCandidateId());
            if (resume == null || resume.getResumeFile() == null || resume.getResumeFile().isBlank()) {
                log.info("Candidate {} has no resume to migrate", entity.getSourceCandidateId());
                return;
            }
            targetService.createCandidateResume(targetCandidateId, resume.getResumeFile());
            log.info("Candidate {}: resume migrated to target candidate {}", entity.getSourceCandidateId(),
                    targetCandidateId);
        } catch (Exception e) {
            log.warn("Candidate {}: failed to migrate resume: {}", entity.getSourceCandidateId(), e.getMessage());
        }
    }

    private void migrateAttachments(CandidateMigration entity, int targetCandidateId) {
        List<CandidateSource.Attachment> attachments;
        try {
            attachments = sourceService.getCandidateAttachments(entity.getSourceCandidateId());
        } catch (Exception e) {
            log.warn("Candidate {}: failed to load source attachments: {}", entity.getSourceCandidateId(),
                    e.getMessage());
            return;
        }
        if (attachments.isEmpty()) {
            log.info("Candidate {} has no attachments to migrate", entity.getSourceCandidateId());
            return;
        }
        int uploaded = 0;
        for (CandidateSource.Attachment attachment : attachments) {
            if (attachment.getFile() == null || attachment.getFile().isBlank()) {
                continue;
            }
            try {
                String name = ensureFileExtension(attachment.getName(), attachment.getFile());
                targetService.createCandidateAttachment(targetCandidateId, name,
                        attachment.getFile(), attachment.getDescription());
                uploaded++;
            } catch (Exception e) {
                log.warn("Candidate {}: failed to upload attachment '{}': {}", entity.getSourceCandidateId(),
                        attachment.getName(), e.getMessage());
            }
        }
        log.info("Candidate {}: uploaded {}/{} attachments to target candidate {}", entity.getSourceCandidateId(),
                uploaded, attachments.size(), targetCandidateId);
    }

    private String ensureFileExtension(String name, String fileUrl) {
        String extension = fileExtensionOf(fileUrl);
        if (extension == null || name == null || name.isBlank()) return name;
        if (name.toLowerCase().endsWith(extension.toLowerCase())) return name;
        return name + extension;
    }

    private String fileExtensionOf(String fileUrl) {
        try {
            String path = java.net.URI.create(fileUrl).getPath();
            if (path == null) return null;
            int idx = path.lastIndexOf('.');
            if (idx < 0 || idx == path.length() - 1) return null;
            String ext = path.substring(idx);
            if (ext.matches("\\.[A-Za-z0-9]{1,10}")) return ext;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private Integer findPipelineStageIdByStageName(int pipelineId, String stageName) {
        try {
            String pipelineJson = targetService.getJobPipeline(pipelineId);
            JsonNode stages = objectMapper.readTree(pipelineJson).path("job_pipeline_stages");
            if (stages.isArray()) {
                List<String> available = new ArrayList<>();
                for (JsonNode stage : stages) {
                    String rawName = stage.path("name").asString();
                    available.add(rawName);
                    if (stageName.equalsIgnoreCase(normalizeStageName(rawName)) && stage.has("id")) {
                        return stage.path("id").asInt();
                    }
                }
                log.warn("Stage '{}' not found in pipeline {}. Available stages: {}",
                        stageName, pipelineId, available);
            }
        } catch (Exception e) {
            log.warn("Failed to resolve stage '{}' for pipeline {}: {}", stageName, pipelineId, e.getMessage());
        }
        return null;
    }

    private String normalizeStageName(String name) {
        if (name == null) {
            return null;
        }
        return name.replaceFirst("^\\d+\\s*-\\s*", "");
    }

    private Integer parsePipelineIdFromMatch(String response) {
        JsonNode node = objectMapper.readTree(response).path("job_pipeline_stage").path("job_pipeline").path("id");
        return node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    private long parseTargetCandidateId(String response) {
        return objectMapper.readTree(response).path("id").asLong();
    }

    private int parseTargetMatchId(String response) {
        return objectMapper.readTree(response).path("id").asInt();
    }

    private String preview(String content) {
        if (content.length() <= 60) return content;
        return content.substring(0, 60) + "...";
    }

    private void markErro(CandidateMigration entity, String message) {
        entity.setStatus("ERRO");
        entity.setErrorMessage(message);
        repository.save(entity);
        log.error("Candidate {} marked as ERRO due to: {}", entity.getSourceCandidateId(), message);
    }
}
