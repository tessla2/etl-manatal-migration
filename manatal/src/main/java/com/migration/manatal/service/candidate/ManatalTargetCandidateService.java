package com.migration.manatal.service.candidate;

import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.NonRetryableApiException;
import com.migration.manatal.model.candidate.CandidateTarget;
import com.migration.manatal.service.ManatalApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManatalTargetCandidateService {

    private final ManatalApiClient apiClient;

    @Value("${migration.manatal.target.token}")
    private String targetToken;

    public String migrateCandidate(CandidateTarget transformed) {
        String url = apiClient.endpoint("/candidates/");
        return apiClient.post(url, transformed, targetToken, "migrating candidate");
    }

    public String createCandidateNote(int candidateId, String content) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/notes/");
        return apiClient.post(url, Map.of("info", content), targetToken, "creating note for candidate " + candidateId);
    }

    public String createCandidateNationality(int candidateId, String country) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/nationalities/");
        return apiClient.post(url, Map.of("country", country), targetToken, "creating nationality for candidate " + candidateId);
    }

    public String createCandidateResume(int candidateId, String resumeFileUrl) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/resume/");
        return apiClient.post(url, Map.of("resume_file", resumeFileUrl), targetToken,
                "creating resume for candidate " + candidateId);
    }

    public String createCandidateAttachment(int candidateId, String name, String fileUrl, String description) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/attachments/");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("file", fileUrl);
        if (description != null && !description.isBlank()) {
            body.put("description", description);
        }
        return apiClient.post(url, body, targetToken, "creating attachment for candidate " + candidateId);
    }

    public String createCandidateSocialMedia(int candidateId, String socialMedia, String socialMediaUrl) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/social-media/");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("social_media", socialMedia);
        body.put("social_media_url", socialMediaUrl);
        return apiClient.post(url, body, targetToken, "creating social media for candidate " + candidateId);
    }

    public String addCandidateSkills(int candidateId, List<CandidateTarget.TargetSkill> skills) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/skills/bulk/");
        return apiClient.post(url, Map.of("skills", skills), targetToken, "adding skills to candidate " + candidateId);
    }

    public String createCandidateMatch(int candidateId, int jobId) {
        String url = apiClient.endpoint("/matches/");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("job", jobId);
        body.put("candidate", candidateId);
        return apiClient.post(url, body, targetToken, "creating match for candidate " + candidateId + " and job " + jobId);
    }

    public String updateMatchStage(int matchId, int stageId) {
        String url = apiClient.endpoint("/matches/" + matchId + "/");
        return apiClient.patch(url, Map.of("job_pipeline_stage", Map.of("id", stageId)), targetToken,
                "updating stage for match " + matchId);
    }

    public String dropMatch(int matchId, String droppedAt) {
        String url = apiClient.endpoint("/matches/" + matchId + "/");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("is_active", false);
        body.put("dropped_at", droppedAt);
        return apiClient.patch(url, body, targetToken, "dropping match " + matchId);
    }

    public String createMatchNote(int matchId, String content) {
        String url = apiClient.endpoint("/matches/" + matchId + "/notes/");
        return apiClient.post(url, Map.of("info", content), targetToken, "creating note for match " + matchId);
    }

    public String getJobPipelines() {
        String url = apiClient.endpoint("/job-pipelines/");
        return apiClient.get(url, targetToken, "listing job pipelines");
    }

    public String getJobPipeline(int pipelineId) {
        String url = apiClient.endpoint("/job-pipelines/" + pipelineId + "/");
        return apiClient.get(url, targetToken, "fetching job pipeline " + pipelineId);
    }

    public String getTargetJob(int jobId) {
        String url = apiClient.endpoint("/jobs/" + jobId + "/");
        return apiClient.get(url, targetToken, "fetching target job " + jobId);
    }

    public String getCandidateMatches(int candidateId) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/matches/");
        return apiClient.get(url, targetToken, "listing matches for candidate " + candidateId);
    }

    public String fetchCandidateById(String candidateId) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/");
        try {
            return apiClient.get(url, targetToken, "fetching candidate by ID");
        } catch (ApiException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                throw new NonRetryableApiException(HttpStatus.NOT_FOUND,
                        "Candidate not found: " + candidateId);
            }
            throw e;
        }
    }
}
