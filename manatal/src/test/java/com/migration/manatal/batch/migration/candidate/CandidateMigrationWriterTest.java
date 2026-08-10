package com.migration.manatal.batch.migration.candidate;

import com.migration.manatal.config.OwnerMappingProperties;
import com.migration.manatal.entity.candidate.CandidateMigration;
import com.migration.manatal.entity.job.JobMigration;
import com.migration.manatal.model.candidate.CandidateSource;
import com.migration.manatal.model.candidate.CandidateTarget;
import com.migration.manatal.repository.candidate.CandidateMigrationRepository;
import com.migration.manatal.repository.job.JobMigrationRepository;
import com.migration.manatal.service.candidate.ManatalSourceCandidateService;
import com.migration.manatal.service.candidate.ManatalTargetCandidateService;
import com.migration.manatal.transform.StageNameMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.Chunk;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateMigrationWriterTest {

    @Mock
    private ManatalTargetCandidateService targetService;

    @Mock
    private ManatalSourceCandidateService sourceService;

    @Mock
    private JobMigrationRepository jobRepository;

    @Mock
    private CandidateMigrationRepository repository;

    @Captor
    private ArgumentCaptor<CandidateMigration> entityCaptor;

    private CandidateMigrationWriter writer;

    @BeforeEach
    void setUp() {
        writer = new CandidateMigrationWriter(targetService, sourceService, jobRepository, repository,
                new ObjectMapper(), stageNameMapper(Map.of()));
    }

    private StageNameMapper stageNameMapper(Map<String, String> mapping) {
        OwnerMappingProperties properties = new OwnerMappingProperties();
        properties.setStageMapping(new java.util.HashMap<>(mapping));
        return new StageNameMapper(properties);
    }

    private CandidateMigration entity(String sourceId) {
        var entity = new CandidateMigration();
        entity.setSourceCandidateId(sourceId);
        entity.setStatus("PENDENTE");
        return entity;
    }

    private CandidateMigrationPackage pkg(CandidateMigration entity) {
        var pkg = new CandidateMigrationPackage();
        pkg.setEntity(entity);
        return pkg;
    }

    @Test
    void shouldWriteCandidateWithSubResourcesAndMatch() throws Exception {
        var entity = entity("42");
        var target = new CandidateTarget();
        target.setFullName("Ana Silva");
        target.setNotes(List.of(note("nota")));
        target.setNationalities(List.of(nationality("Portugal")));
        target.setSkills(List.of(skill("Java", 7)));
        var pkg = pkg(entity);
        pkg.setTransformed(target);

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateCandidate(target)).thenReturn("{\"id\": 55}");
        when(sourceService.getCandidateMatches("42")).thenReturn(List.of(match(101L, "Entrevista", true, null)));
        when(jobRepository.findBySourceJobId("101"))
                .thenReturn(Optional.of(jobWithTargetId(501L)));
        when(targetService.createCandidateMatch(55, 501)).thenReturn(
                "{\"id\": 77, \"job_pipeline_stage\": {\"id\": 306970, \"job_pipeline\": {\"id\": 29324}, \"name\": \"Rascunho\", \"rank\": 1}}");
        when(targetService.getJobPipeline(29324)).thenReturn(
                "{\"id\": 29324, \"name\": \"Default Job Pipeline\", \"job_pipeline_stages\": ["
                        + "{\"id\": 306971, \"name\": \"Nova candidatura\", \"rank\": 1},"
                        + "{\"id\": 306972, \"name\": \"Entrevista\", \"rank\": 2}]}");
        when(sourceService.getCandidateResume("42"))
                .thenReturn(resume("https://s3/cv.pdf"));
        when(sourceService.getCandidateAttachments("42"))
                .thenReturn(List.of(attachment("doc", "https://s3/doc.pdf", "contrato")));

        writer.write(new Chunk<>(pkg));

        verify(targetService).migrateCandidate(target);
        verify(targetService).createCandidateNote(55, "nota");
        verify(targetService).createCandidateNationality(55, "Portugal");
        verify(targetService).addCandidateSkills(eq(55), anyList());
        verify(targetService).createCandidateMatch(55, 501);
        verify(targetService).updateMatchStage(77, 306972);
        verify(targetService, never()).dropMatch(anyInt(), anyString());
        verify(targetService).createCandidateResume(55, "https://s3/cv.pdf");
        verify(targetService).createCandidateAttachment(55, "doc.pdf", "https://s3/doc.pdf", "contrato");

        assertEquals("SUCESSO", entity.getStatus());
        assertEquals(55L, entity.getTargetCandidateId());
        assertNull(entity.getErrorMessage());
        assertEquals("101", entity.getSourceJobId());
        assertEquals(501L, entity.getTargetJobId());
        assertEquals("Entrevista", entity.getStageName());
    }

    @Test
    void shouldTranslateStageNameBeforeMatchingOnTargetPipeline() throws Exception {
        var entity = entity("42");
        var pkg = pkg(entity);
        pkg.setTransformed(new CandidateTarget());

        var stageWriter = new CandidateMigrationWriter(targetService, sourceService, jobRepository, repository,
                new ObjectMapper(), stageNameMapper(Map.of("HR Round", "Recruiter Interviews")));

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateCandidate(any())).thenReturn("{\"id\": 55}");
        when(sourceService.getCandidateMatches("42"))
                .thenReturn(List.of(match(101L, "HR Round", true, null)));
        when(jobRepository.findBySourceJobId("101"))
                .thenReturn(Optional.of(jobWithTargetId(501L)));
        when(targetService.createCandidateMatch(55, 501)).thenReturn(
                "{\"id\": 77, \"job_pipeline_stage\": {\"id\": 306970, \"job_pipeline\": {\"id\": 29324}, \"name\": \"Rascunho\", \"rank\": 1}}");
        when(targetService.getJobPipeline(29324)).thenReturn(
                "{\"id\": 29324, \"name\": \"Default Job Pipeline\", \"job_pipeline_stages\": ["
                        + "{\"id\": 306971, \"name\": \"Nova candidatura\", \"rank\": 1},"
                        + "{\"id\": 306973, \"name\": \"Recruiter Interviews\", \"rank\": 2}]}");
        when(sourceService.getCandidateResume("42")).thenReturn(null);
        when(sourceService.getCandidateAttachments("42")).thenReturn(List.of());

        stageWriter.write(new Chunk<>(pkg));

        verify(targetService).updateMatchStage(77, 306973);
        assertEquals("HR Round", entity.getStageName());
        assertEquals(501L, entity.getTargetJobId());
    }

    @Test
    void shouldDropMatchWithNoteWhenInactive() throws Exception {
        var entity = entity("42");
        var pkg = pkg(entity);
        pkg.setTransformed(new CandidateTarget());

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateCandidate(any())).thenReturn("{\"id\": 55}");
        when(sourceService.getCandidateMatches("42"))
                .thenReturn(List.of(match(101L, "Rascunho", false, "2026-08-05T10:00:00")));
        when(jobRepository.findBySourceJobId("101"))
                .thenReturn(Optional.of(jobWithTargetId(501L)));
        when(targetService.createCandidateMatch(55, 501)).thenReturn(
                "{\"id\": 77, \"job_pipeline_stage\": {\"id\": 306970, \"job_pipeline\": {\"id\": 29324}, \"name\": \"Rascunho\", \"rank\": 1}}");
        when(targetService.getJobPipeline(29324)).thenReturn(
                "{\"id\": 29324, \"name\": \"Default Job Pipeline\", \"job_pipeline_stages\": "
                        + "[{\"id\": 306971, \"name\": \"Rascunho\", \"rank\": 1}]}");
        when(sourceService.getCandidateResume("42")).thenReturn(null);
        when(sourceService.getCandidateAttachments("42")).thenReturn(List.of());

        writer.write(new Chunk<>(pkg));

        verify(targetService).dropMatch(77, "2026-08-05T10:00:00");
        verify(targetService).createMatchNote(77, "Dropado em 2026-08-05T10:00:00 do stage Rascunho");
        assertEquals("101", entity.getSourceJobId());
        assertEquals(501L, entity.getTargetJobId());
        assertEquals("Rascunho", entity.getStageName());
    }

    @Test
    void shouldKeepDefaultStageWhenStageNameNotFound() throws Exception {
        var entity = entity("42");
        var pkg = pkg(entity);
        pkg.setTransformed(new CandidateTarget());

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateCandidate(any())).thenReturn("{\"id\": 55}");
        when(sourceService.getCandidateMatches("42"))
                .thenReturn(List.of(match(101L, "Etapa inexistente", true, null)));
        when(jobRepository.findBySourceJobId("101"))
                .thenReturn(Optional.of(jobWithTargetId(501L)));
        when(targetService.createCandidateMatch(55, 501)).thenReturn(
                "{\"id\": 77, \"job_pipeline_stage\": {\"id\": 306970, \"job_pipeline\": {\"id\": 29324}, \"name\": \"Rascunho\", \"rank\": 1}}");
        when(targetService.getJobPipeline(29324)).thenReturn(
                "{\"id\": 29324, \"name\": \"Default Job Pipeline\", \"job_pipeline_stages\": "
                        + "[{\"id\": 306971, \"name\": \"Rascunho\", \"rank\": 1}]}");
        when(sourceService.getCandidateResume("42")).thenReturn(null);
        when(sourceService.getCandidateAttachments("42")).thenReturn(List.of());

        writer.write(new Chunk<>(pkg));

        verify(targetService).createCandidateMatch(55, 501);
        verify(targetService, never()).updateMatchStage(anyInt(), anyInt());
    }

    @Test
    void shouldMatchStageIgnoringRankPrefix() throws Exception {
        var entity = entity("42");
        var pkg = pkg(entity);
        pkg.setTransformed(new CandidateTarget());

        var stageWriter = new CandidateMigrationWriter(targetService, sourceService, jobRepository, repository,
                new ObjectMapper(), stageNameMapper(Map.of("Analyzed", "Pre-selection")));

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateCandidate(any())).thenReturn("{\"id\": 55}");
        when(sourceService.getCandidateMatches("42"))
                .thenReturn(List.of(match(101L, "Analyzed", true, null)));
        when(jobRepository.findBySourceJobId("101"))
                .thenReturn(Optional.of(jobWithTargetId(501L)));
        when(targetService.createCandidateMatch(55, 501)).thenReturn(
                "{\"id\": 77, \"job_pipeline_stage\": {\"id\": 306970, \"job_pipeline\": {\"id\": 29324}, \"name\": \"1 - Pre-selection\", \"rank\": 1}}");
        when(targetService.getJobPipeline(29324)).thenReturn(
                "{\"id\": 29324, \"name\": \"Default Job Pipeline\", \"job_pipeline_stages\": ["
                        + "{\"id\": 306971, \"name\": \"1 - Pre-selection\", \"rank\": 1},"
                        + "{\"id\": 306972, \"name\": \"2 - Contacted\", \"rank\": 2}]}");
        when(sourceService.getCandidateResume("42")).thenReturn(null);
        when(sourceService.getCandidateAttachments("42")).thenReturn(List.of());

        stageWriter.write(new Chunk<>(pkg));

        verify(targetService).updateMatchStage(77, 306971);
    }

    @Test
    void shouldSkipMatchWhenSourceJobNotMigrated() throws Exception {
        var entity = entity("42");
        var pkg = pkg(entity);
        pkg.setTransformed(new CandidateTarget());

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateCandidate(any())).thenReturn("{\"id\": 55}");
        when(sourceService.getCandidateMatches("42"))
                .thenReturn(List.of(match(101L, "Entrevista", true, null)));
        when(jobRepository.findBySourceJobId("101")).thenReturn(Optional.empty());
        when(sourceService.getCandidateResume("42")).thenReturn(null);
        when(sourceService.getCandidateAttachments("42")).thenReturn(List.of());

        writer.write(new Chunk<>(pkg));

        verify(targetService, never()).createCandidateMatch(anyInt(), anyInt());
    }

    @Test
    void shouldKeepSuccessWhenSubResourcePostingFails() throws Exception {
        var entity = entity("42");
        var target = new CandidateTarget();
        target.setFullName("Ana Silva");
        target.setNotes(List.of(note("nota")));
        var pkg = pkg(entity);
        pkg.setTransformed(target);

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateCandidate(target)).thenReturn("{\"id\": 55}");
        doThrow(new RuntimeException("note error")).when(targetService).createCandidateNote(anyInt(), anyString());
        when(sourceService.getCandidateMatches("42")).thenThrow(new RuntimeException("matches error"));
        when(sourceService.getCandidateResume("42")).thenThrow(new RuntimeException("resume error"));
        when(sourceService.getCandidateAttachments("42")).thenThrow(new RuntimeException("attachments error"));

        writer.write(new Chunk<>(pkg));

        assertEquals("SUCESSO", entity.getStatus());
        assertEquals(55L, entity.getTargetCandidateId());
    }

    @Test
    void shouldMarkErroWhenTargetPostFails() throws Exception {
        var entity = entity("42");
        var pkg = pkg(entity);
        pkg.setTransformed(new CandidateTarget());

        when(targetService.migrateCandidate(any())).thenThrow(new RuntimeException("target error"));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        writer.write(new Chunk<>(pkg));

        verify(repository).save(entityCaptor.capture());
        var saved = entityCaptor.getValue();
        assertEquals("ERRO", saved.getStatus());
        assertEquals("target error", saved.getErrorMessage());
    }

    @Test
    void shouldSkipItemWithPreSetError() throws Exception {
        var entity = entity("42");
        var pkg = pkg(entity);
        pkg.setErrorMessage("previous error");

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        writer.write(new Chunk<>(pkg));

        verify(targetService, never()).migrateCandidate(any());
        verify(repository).save(entityCaptor.capture());
        assertEquals("ERRO", entityCaptor.getValue().getStatus());
        assertEquals("previous error", entityCaptor.getValue().getErrorMessage());
    }

    @Test
    void shouldSkipItemAlreadyMigratedInDb() throws Exception {
        var entity = entity("42");
        entity.setTargetCandidateId(55L);
        entity.setStatus("SUCESSO");
        var pkg = pkg(entity);

        writer.write(new Chunk<>(pkg));

        verify(targetService, never()).migrateCandidate(any());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldAppendFileExtensionToAttachmentName() throws Exception {
        var entity = entity("42");
        var pkg = pkg(entity);
        pkg.setTransformed(new CandidateTarget());

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateCandidate(any())).thenReturn("{\"id\": 55}");
        when(sourceService.getCandidateMatches("42")).thenReturn(List.of());
        when(sourceService.getCandidateResume("42")).thenReturn(null);
        when(sourceService.getCandidateAttachments("42"))
                .thenReturn(List.of(
                        attachment("CV", "https://s3/cv.PDF", null),
                        attachment("doc.pdf", "https://s3/doc.pdf", null),
                        attachment("semext", "https://s3/arquivo", null)));

        writer.write(new Chunk<>(pkg));

        verify(targetService).createCandidateAttachment(55, "CV.PDF", "https://s3/cv.PDF", null);
        verify(targetService).createCandidateAttachment(55, "doc.pdf", "https://s3/doc.pdf", null);
        verify(targetService).createCandidateAttachment(55, "semext", "https://s3/arquivo", null);
    }

    private CandidateTarget.TargetNote note(String content) {
        var note = new CandidateTarget.TargetNote();
        note.setContent(content);
        return note;
    }

    private CandidateTarget.TargetNationality nationality(String country) {
        var nationality = new CandidateTarget.TargetNationality();
        nationality.setCountry(country);
        return nationality;
    }

    private CandidateTarget.TargetSkill skill(String name, int score) {
        var skill = new CandidateTarget.TargetSkill();
        skill.setSkillName(name);
        skill.setScore(score);
        return skill;
    }

    private CandidateSource.CandidateMatch match(Long jobId, String stageName, boolean active, String droppedAt) {
        var match = new CandidateSource.CandidateMatch();
        match.setId(9L);
        match.setJob(jobId);
        match.setIsActive(active);
        match.setDroppedAt(droppedAt);
        var stage = new CandidateSource.CandidateMatch.Stage();
        stage.setName(stageName);
        match.setStage(stage);
        return match;
    }

    private JobMigration jobWithTargetId(Long targetId) {
        var job = new JobMigration();
        job.setTargetJobId(targetId);
        return job;
    }

    @Test
    void shouldMigrateCandidateSocialMedia() throws Exception {
        var entity = entity("42");
        var pkg = pkg(entity);
        pkg.setTransformed(new CandidateTarget());

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateCandidate(any())).thenReturn("{\"id\": 55}");
        when(sourceService.getCandidateMatches("42")).thenReturn(List.of());
        when(sourceService.getCandidateResume("42")).thenReturn(null);
        when(sourceService.getCandidateAttachments("42")).thenReturn(List.of());
        when(sourceService.getCandidateSocialMedia("42"))
                .thenReturn(List.of(socialMedia("linkedin", "https://linkedin.com/in/user")));

        writer.write(new Chunk<>(pkg));

        verify(targetService).createCandidateSocialMedia(55, "linkedin", "https://linkedin.com/in/user");
    }

    @Test
    void shouldMigrateActivitiesAsNotes() throws Exception {
        var entity = entity("42");
        var pkg = pkg(entity);
        pkg.setTransformed(new CandidateTarget());

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateCandidate(any())).thenReturn("{\"id\": 55}");
        when(sourceService.getCandidateMatches("42")).thenReturn(List.of());
        when(sourceService.getCandidateSocialMedia("42")).thenReturn(List.of());
        when(sourceService.getCandidateResume("42")).thenReturn(null);
        when(sourceService.getCandidateAttachments("42")).thenReturn(List.of());
        when(sourceService.getCandidateActivities("42"))
                .thenReturn(List.of(activity("Entrevista", "2026-08-05T10:00:00", "Candidato foi bem", 5)));
        when(sourceService.listUsersBestEffort()).thenReturn(Map.of(5, "Ana Silva"));

        writer.write(new Chunk<>(pkg));

        verify(targetService).createCandidateNote(55, "Entrevista - 2026-08-05 - Ana Silva: Candidato foi bem");
    }

    @Test
    void shouldMigrateActivityNoteWithoutCreatorName() throws Exception {
        var entity = entity("42");
        var pkg = pkg(entity);
        pkg.setTransformed(new CandidateTarget());

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(targetService.migrateCandidate(any())).thenReturn("{\"id\": 55}");
        when(sourceService.getCandidateMatches("42")).thenReturn(List.of());
        when(sourceService.getCandidateSocialMedia("42")).thenReturn(List.of());
        when(sourceService.getCandidateResume("42")).thenReturn(null);
        when(sourceService.getCandidateAttachments("42")).thenReturn(List.of());
        when(sourceService.getCandidateActivities("42"))
                .thenReturn(List.of(activity("Entrevista", "2026-08-05T10:00:00", "Candidato foi bem", 5)));
        when(sourceService.listUsersBestEffort()).thenReturn(Map.of());

        writer.write(new Chunk<>(pkg));

        verify(targetService).createCandidateNote(55, "Entrevista - 2026-08-05: Candidato foi bem");
    }

    private CandidateSource.Activity activity(String name, String dueDate, String description, Integer creator) {
        var activity = new CandidateSource.Activity();
        activity.setName(name);
        activity.setDueDate(dueDate);
        activity.setDescription(description);
        activity.setCreator(creator);
        return activity;
    }

    private CandidateSource.SocialMedia socialMedia(String type, String url) {
        var socialMedia = new CandidateSource.SocialMedia();
        socialMedia.setSocialMedia(type);
        socialMedia.setSocialMediaUrl(url);
        return socialMedia;
    }

    private CandidateSource.Resume resume(String file) {
        var resume = new CandidateSource.Resume();
        resume.setResumeFile(file);
        return resume;
    }

    private CandidateSource.Attachment attachment(String name, String file, String description) {
        var attachment = new CandidateSource.Attachment();
        attachment.setName(name);
        attachment.setFile(file);
        attachment.setDescription(description);
        return attachment;
    }
}
