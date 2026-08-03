package com.migration.manatal.service.job;

import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.NonRetryableApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.job.JobTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManatalTargetJobService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${migration.manatal.base-url}")
    private String baseUrl;

    @Value("${migration.manatal.target.token}")
    private String targetToken;

    @Value("${migration.manatal.rate-limit-retry-seconds:60}")
    private int rateLimitRetrySeconds;

    @Value("${migration.batch.retry-limit:3}")
    private int retryLimit;

    public String createJobNote(int jobId, String content) {
        String url = normalizedBaseUrl() + "/jobs/" + jobId + "/notes/";
        ObjectNode body = objectMapper.createObjectNode();
        body.put("info", content);
        return sendPostRequest(url, body, "creating note for job " + jobId);
    }

    //============================== HTTP  =================================

    private String sendPostRequest(String url, Object body, String context) {
        int attempt = 0;
        while (true) {
            try {
                String json = objectMapper.writeValueAsString(body);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Token " + targetToken)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == HttpStatus.CREATED.value() || response.statusCode() == HttpStatus.OK.value()) {
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

    public String migrateJob(JobTarget transformed) {
        String url = normalizedBaseUrl() + "/jobs/";
        return sendPostRequest(url, transformed, "migrating job");
    }
}
