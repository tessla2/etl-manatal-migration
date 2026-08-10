package com.migration.manatal.service.job;


import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.job.JobSource;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.service.ManatalApiClient;
import com.migration.manatal.transform.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManatalSourceJobService {

    private final ObjectMapper objectMapper;
    private final JobMapper jobMapper;
    private final ManatalApiClient apiClient;

    @Value("${migration.manatal.source.token}")
    private String token;

    @Value("${migration.manatal.page-size:100}")
    private int pageSize;

    //============================== Jobs =================================

    public String listJobs(int offset) {
        String url = apiClient.endpoint("/jobs/?limit=" + pageSize + "&offset=" + offset);
        return apiClient.get(url, token, "fetching Jobs");
    }

    public String listJobsPage(int page) {
        String url = apiClient.endpoint("/jobs/?page=" + page + "&page_size=" + pageSize);
        return apiClient.get(url, token, "fetching jobs page " + page);
    }

    public List<JobExportInfo> listJobWithExportedFilter() {
        List<JobExportInfo> result = new java.util.ArrayList<>();
        int page = 1;
        try {
            while (true) {
                String json = listJobsPage(page);
                var root = objectMapper.readTree(json);
                var results = root.path("results");

                if (!results.isArray() || results.isEmpty()) {
                    log.info("Jobs loader: page {} is empty, stopping", page);
                    break;
                }

                int toExportOnPage = 0;
                for (var job : results) {
                    if ("To Export".equals(job.path("custom_fields").path("exported").asString(""))) {
                        result.add(new JobExportInfo(
                                job.path("id").asString(),
                                job.path("position_name").asString("")));
                        toExportOnPage++;
                    }
                }
                log.info("Jobs loader: page {} returned {} job(s), {} with exported = 'To Export' (total so far: {})",
                        page, results.size(), toExportOnPage, result.size());

                if (results.size() < pageSize) {
                    break;
                }
                page++;
            }
            log.info("Jobs loader: complete, {} job(s) with exported = 'To Export'", result.size());
            return result;
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error listing jobs with export filter", e);
            throw ApiException.badGateway("Error listing jobs with export filter", e);
        }
    }

    public record JobExportInfo(String id, String positionName) {}

    public String getJobById(String jobId) {
        String url = apiClient.endpoint("/jobs/" + jobId);
        return apiClient.get(url, token, "fetching Job by ID");
    }

    public JobTarget previewJobMigrated(String jobId) {
        log.info("Job {}: fetching source job for preview...", jobId);
        String jobJson = getJobById(jobId);
        try {
            JobSource source = objectMapper.readValue(jobJson, JobSource.class);

            String notesJson = listJobNotes(jobId, 0);
            List<JobSource.JobNote> notes = apiClient.parseResultsList(notesJson, JobSource.JobNote.class);
            log.info("Job {}: source loaded (position='{}', organization={}), {} note(s) found",
                    jobId, source.getPositionName(), source.getOrganization(), notes.size());

            return jobMapper.toTarget(source, notes);
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing job {} for preview", jobId, e);
            throw ApiException.badGateway("Error processing job " + jobId + " for preview", e);
        }
    }

    public String listJobNotes(String jobId, int offset) {
        String url = apiClient.endpoint("/jobs/" + jobId + "/notes/?limit=" + pageSize + "&offset=" + offset);
        return apiClient.get(url, token, "fetching notes for job " + jobId);
    }

    public List<JobContactInfo> listJobsWithContacts() {
        List<JobContactInfo> result = new java.util.ArrayList<>();
        int page = 1;
        try {
            while (true) {
                String json = listJobsPage(page);
                List<JobSource> jobs = apiClient.parseResultsList(json, JobSource.class);
                for (JobSource job : jobs) {
                    if (job.getCustomFields() != null
                            && job.getCustomFields().getContactName() != null
                            && !job.getCustomFields().getContactName().isBlank()) {
                        result.add(new JobContactInfo(
                                job.getId(),
                                job.getPositionName(),
                                job.getCustomFields().getContactName()));
                    }
                }
                if (jobs.size() < pageSize) break;
                page++;
            }
            return result;
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error listing jobs with contacts", e);
            throw ApiException.badGateway("Error listing jobs with contacts", e);
        }
    }

    public void updateCustomField(String sourceJobId, String field, String value) {
        String url = apiClient.endpoint("/jobs/" + sourceJobId + "/");
        Map<String, Object> customFields = new HashMap<>(fetchCustomFields(sourceJobId));
        customFields.put(field, value);
        Map<String, Object> body = Map.of("custom_fields", customFields);
        apiClient.patch(url, body, token, "updating custom field for job " + sourceJobId);
    }

    private Map<String, Object> fetchCustomFields(String sourceJobId) {
        try {
            JsonNode root = objectMapper.readTree(getJobById(sourceJobId));
            JsonNode customFields = root.path("custom_fields");
            if (customFields.isMissingNode() || customFields.isNull()) {
                return new HashMap<>();
            }
            return objectMapper.convertValue(customFields, new TypeReference<Map<String, Object>>() {});
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error reading custom fields for job {}", sourceJobId, e);
            throw ApiException.badGateway("Error reading custom fields for job " + sourceJobId, e);
        }
    }

    public record JobContactInfo(Integer id, String positionName, String contactName) {}

}
