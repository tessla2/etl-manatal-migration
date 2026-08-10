package com.migration.manatal.service;

import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.NonRetryableApiException;
import com.migration.manatal.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ManatalApiClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${migration.manatal.base-url}")
    private String baseUrl;

    @Value("${migration.manatal.rate-limit-retry-seconds:60}")
    private int rateLimitRetrySeconds;

    @Value("${migration.batch.retry-limit:3}")
    private int retryLimit;

    public String get(String url, String token, String context) {
        return execute(url, token, context, "GET", null);
    }

    public String post(String url, Object body, String token, String context) {
        return execute(url, token, context, "POST", body);
    }

    public String patch(String url, Object body, String token, String context) {
        return execute(url, token, context, "PATCH", body);
    }

    public String delete(String url, String token, String context) {
        return execute(url, token, context, "DELETE", null);
    }

    public String endpoint(String path) {
        return baseUrl.replaceAll("/+$", "") + path;
    }

    public <T> List<T> parseResultsList(String json, Class<T> elementType) throws Exception {
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

    private String sanitizeUrl(String url) {
        String clean = url;
        int q = clean.indexOf('?');
        if (q >= 0) {
            String[] pairs = clean.substring(q + 1).split("&");
            StringBuilder safe = new StringBuilder(clean.substring(0, q + 1));
            boolean first = true;
            for (String pair : pairs) {
                String key = pair.split("=", 2)[0];
                if ("token".equalsIgnoreCase(key) || "api_key".equalsIgnoreCase(key) || "Authorization".equalsIgnoreCase(key)) {
                    continue;
                }
                if (!first) {
                    safe.append('&');
                }
                safe.append(pair);
                first = false;
            }
            clean = safe.toString();
        }
        return clean;
    }

    // ============================= HTTP =============================

    private String execute(String url, String token, String context, String method, Object body) {
        int attempt = 0;
        while (true) {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Authorization", "Token " + token);
                if (body != null) {
                    String json = objectMapper.writeValueAsString(body);
                    builder.header("Content-Type", "application/json")
                            .method(method, HttpRequest.BodyPublishers.ofString(json));
                } else {
                    builder.method(method, HttpRequest.BodyPublishers.noBody());
                }

                long start = System.currentTimeMillis();
                HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                log.debug("HTTP {} {} -> {} ({} ms)", method, sanitizeUrl(url), response.statusCode(),
                        System.currentTimeMillis() - start);

                if (isSuccess(response.statusCode())) {
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

    private boolean isSuccess(int code) {
        return code == HttpStatus.OK.value()
                || code == HttpStatus.CREATED.value()
                || code == 204;
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
}
