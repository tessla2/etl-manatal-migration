package com.migration.manatal.service.job;

import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.service.ManatalApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManatalTargetJobService {

    private final ManatalApiClient apiClient;

    @Value("${migration.manatal.target.token}")
    private String targetToken;

    public String createJobNote(int jobId, String content) {
        String url = apiClient.endpoint("/jobs/" + jobId + "/notes/");
        return apiClient.post(url, Map.of("info", content), targetToken, "creating note for job " + jobId);
    }

    public String migrateJob(JobTarget transformed) {
        String url = apiClient.endpoint("/jobs/");
        return apiClient.post(url, transformed, targetToken, "migrating job");
    }

    public String getJobById(String jobId) {
        String url = apiClient.endpoint("/jobs/" + jobId);
        return apiClient.get(url, targetToken, "fetching target Job by ID");
    }

    public String getJobPipeline(int pipelineId) {
        String url = apiClient.endpoint("/job-pipelines/" + pipelineId);
        return apiClient.get(url, targetToken, "fetching target job pipeline " + pipelineId);
    }

    public List<IndustryTarget> listIndustries() {
        try {
            String url = apiClient.endpoint("/industries/");
            String json = apiClient.get(url, targetToken, "fetching target industries");
            List<IndustryTarget> industries = apiClient.parseResultsList(json, IndustryTarget.class);
            log.info("Target industries loader: complete, {} industry(ies) loaded", industries.size());
            return industries;
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error parsing target industries list", e);
            throw ApiException.badGateway("Error parsing target industries list", e);
        }
    }

    public record IndustryTarget(Integer id, String name) {}
}
