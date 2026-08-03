package com.migration.manatal.service.job;


import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.NonRetryableApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.job.JobSource;
import com.migration.manatal.model.job.JobTarget;
import com.migration.manatal.transform.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManatalSourceJobService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final JobMapper jobMapper;

    @Value("${migration.manatal.base-url}")
    private String baseUrl;

    @Value("${migration.manatal.source.token}")
    private String token;

    @Value("${migration.manatal.page-size:100}")
    private int pageSize;

    @Value("${migration.manatal.rate-limit-retry-seconds:60}")
    private int rateLimitRetrySeconds;

    @Value("${migration.batch.retry-limit:3}")
    private int retryLimit;

    //============================== Jobs =================================

    public String listJobs(int offset) {
        String url = normalizedBaseUrl() + "/jobs/?limit=" + pageSize + "&offset=" + offset;
        return sendGetRequest(url, "fetching Jobs");
    }

    public String listJobWithExportedFilter(int offset) {
        String url = normalizedBaseUrl() + "/jobs/?limit=" + pageSize + "&offset=" + offset
                + "&custom_fields__exported=" + URLEncoder.encode("To Export", StandardCharsets.UTF_8);
        return sendGetRequest(url, "fetching jobs to export");
    }

    public String getJobById(String jobId) {
        String url = normalizedBaseUrl() + "/jobs/" + jobId;
        return sendGetRequest(url, "fetching Job by ID");
    }

    public JobTarget previewJobMigrated(String jobId) {
        String jobJson = getJobById(jobId);
        try {
            JobSource source = objectMapper.readValue(jobJson, JobSource.class);

            String notesJson = listJobNotes(jobId, 0);
            List<JobSource.JobNote> notes = parseResultsList(notesJson, JobSource.JobNote.class);

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
        String url = normalizedBaseUrl() + "/jobs/" + jobId + "/notes/?limit=" + pageSize + "&offset=" + offset;
        return sendGetRequest(url, "fetching notes for job " + jobId);
    }

    public List<JobContactInfo> listJobsWithContacts() {
        List<JobContactInfo> result = new java.util.ArrayList<>();
        int offset = 0;
        try {
            while (true) {
                String json = listJobs(offset);
                List<JobSource> jobs = parseResultsList(json, JobSource.class);
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
                offset += jobs.size();
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
        String url = normalizedBaseUrl() + "/jobs/" + sourceJobId + "/";
        Map<String, Object> body = Map.of("custom_fields", Map.of(field, value));
        sendPatchRequest(url, body, "updating custom field for job " + sourceJobId);
    }

    public record JobContactInfo(Integer id, String positionName, String contactName) {}

    // ============================= Parsing =============================

    private <T> List<T> parseResultsList(String json, Class<T> elementType) throws Exception {
        var root = objectMapper.readTree(json);
        if (root.isArray()) {
            return objectMapper.readerForListOf(elementType).readValue(root);
        }
        var results = root.get("results");
        if (results != null) {
            return objectMapper.readerForListOf(elementType).readValue(results);
        }
        return List.of();
    }


    // ============================= HTTP =============================

    private String sendGetRequest(String url, String context) {
        int attempt = 0;
        while (true) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Token " + token)
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == HttpStatus.OK.value()) {
                    return response.body();
                }

                if (response.statusCode() == 429) {
                    throw new RateLimitException(retryAfterSeconds(response));
                }

                throw apiExceptionFor(response, context);

            } catch (RateLimitException e) {
                if (++attempt >= retryLimit) throw e;
                log.warn("Rate limited while {} (attempt {}/{}), retrying after {}s",
                        context, attempt, retryLimit, e.getRetryAfterSeconds());
                sleep(e.getRetryAfterSeconds());
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                log.error("Exception while {}", context, e);
                throw ApiException.badGateway("Exception while " + context, e);
            }
        }
    }

    private void sendPatchRequest(String url, Object body, String context) {
        int attempt = 0;
        while (true) {
            try {
                String json = objectMapper.writeValueAsString(body);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Token " + token)
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == HttpStatus.OK.value() || response.statusCode() == 204) {
                    return;
                }

                if (response.statusCode() == 429) {
                    throw new RateLimitException(retryAfterSeconds(response));
                }

                throw apiExceptionFor(response, context);

            } catch (RateLimitException e) {
                if (++attempt >= retryLimit) throw e;
                log.warn("Rate limited while {} (attempt {}/{}), retrying after {}s",
                        context, attempt, retryLimit, e.getRetryAfterSeconds());
                sleep(e.getRetryAfterSeconds());
            } catch (ApiException e) {
                throw e;
            } catch (Exception e) {
                log.error("Exception while {}", context, e);
                throw ApiException.badGateway("Exception while " + context, e);
            }
        }
    }

    private ApiException apiExceptionFor(HttpResponse<String> response, String context) {
        int code = response.statusCode();
        String body = response.body();
        log.error("Error {}: HTTP {} - {}", context, code, body);

        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) {
            return new ApiException(HttpStatus.BAD_GATEWAY,
                    "Unexpected HTTP " + code + " while " + context + ": " + body);
        }
        if (status.is4xxClientError()) {
            return new NonRetryableApiException(status, body);
        }
        return new ApiException(status, body);
    }

    private long retryAfterSeconds(HttpResponse<?> response) {
        var headers = response.headers();
        if (headers != null) {
            var retryAfter = headers.firstValue("Retry-After").orElse(null);
            if (retryAfter != null) {
                try {
                    return Long.parseLong(retryAfter);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse Retry-After header value '{}', using default", retryAfter);
                }
            }
        }
        return rateLimitRetrySeconds;
    }

    private void sleep(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw ApiException.badGateway("Interrupted while waiting for rate limit", e);
        }
    }

    private String normalizedBaseUrl() {
        return baseUrl.replaceAll("/+$", "");
    }

}
