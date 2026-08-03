package com.migration.manatal.service.client;

import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.NonRetryableApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.client.ClientSource;
import com.migration.manatal.model.client.ClientSource.SourceContact;
import com.migration.manatal.model.client.ClientSource.SourceNote;
import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.transform.ClientMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ManatalSourceClientService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ClientMapper clientMapper;

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

    //============================== Organizations =================================

    public String listOrganizations(int offset) {
        String url = normalizedBaseUrl() + "/organizations/?limit=" + pageSize + "&offset=" + offset;
        return sendGetRequest(url, "fetching organizations");
    }

    public String fetchOrganizationById(String organizationId) {
        String url = normalizedBaseUrl() + "/organizations/" + organizationId + "/";
        try {
            return sendGetRequest(url, "fetching organization by ID");
        } catch (ApiException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                throw new NonRetryableApiException(HttpStatus.NOT_FOUND,
                        "Organization not found: " + organizationId);
            }
            throw e;
        }
    }

    public void updateCustomField(String organizationId, String field, String value) {
        String url = normalizedBaseUrl() + "/organizations/" + organizationId + "/";
        Map<String, Object> body = Map.of("custom_fields", Map.of(field, value));
        sendPatchRequest(url, body, "updating custom field for organization " + organizationId);
    }

    public String listOrganizationsWithExportFilter(int offset) {
        String url = normalizedBaseUrl() + "/organizations/?limit=" + pageSize + "&offset=" + offset + "&custom_fields__exported=" + URLEncoder.encode("To Export", StandardCharsets.UTF_8);
        return sendGetRequest(url, "fetching organizations to export");
    }

    public ClientTarget previewClientMigrated(String organizationId) {
        String organizationJson = fetchOrganizationById(organizationId);
        try {
            ClientSource source = objectMapper.readValue(organizationJson, ClientSource.class);

            int organizationIdNumber;
            try {
                organizationIdNumber = Integer.parseInt(organizationId);
            } catch (NumberFormatException e) {
                throw ApiException.badRequest("Invalid organization id: " + organizationId);
            }

            String contactsJson = listContactsByOrganization(organizationIdNumber);
            List<SourceContact> contactSources = parseResultsList(contactsJson, SourceContact.class);

            String notesJson = listOrganizationNotes(organizationId, 0);
            List<SourceNote> noteSources = parseResultsList(notesJson, SourceNote.class);

            return clientMapper.toTarget(source, contactSources, noteSources);
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing organization {} for preview", organizationId, e);
            throw ApiException.badGateway("Error processing organization " + organizationId + " for preview", e);
        }
    }

    //============================= Contacts ===============================

    public String listContacts(int offset) {
        String url = normalizedBaseUrl() + "/contacts/?limit=" + pageSize + "&offset=" + offset;
        return sendGetRequest(url, "fetching contacts");
    }

    public String listContactsByOrganization(int organizationId) {
        String url = normalizedBaseUrl() + "/contacts/?limit=" + pageSize + "&organization_id=" + organizationId;
        return sendGetRequest(url, "fetching contacts for organization " + organizationId);
    }

    // ============================= Notes =============================

    public String listOrganizationNotes(String organizationId, int offset) {
        String url = normalizedBaseUrl() + "/organizations/" + organizationId + "/notes/?limit=" + pageSize + "&offset=" + offset;
        return sendGetRequest(url, "fetching notes for organization " + organizationId);
    }

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
