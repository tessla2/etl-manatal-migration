package com.migration.manatal.service;

import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.ClientSource;
import com.migration.manatal.model.ClientTarget;
import com.migration.manatal.transform.ClientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManatalSourceClientService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ClientMapper clientMapper;

    @Value("${migration.manatal.base-url}")
    private String baseUrl;

    @Value("${migration.manatal.token}")
    private String token;

    @Value("${migration.manatal.page-size:100}")
    private int pageSize;

    public String listOrganizations(int offset) {
        String url = normalizedBaseUrl() + "/organizations/?limit=" + pageSize + "&offset=" + offset;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Token " + token)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpStatus.OK.value()) {
                return response.body();
            } else if (response.statusCode() == 429) {
                throw new RateLimitException(60);
            } else {
                log.error("Error fetching organizations: HTTP {} - {}", response.statusCode(), response.body());
                throw new ApiException(HttpStatus.valueOf(response.statusCode()), response.body());
            }
        } catch (ApiException | RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Exception while fetching organizations", e);
            throw ApiException.badGateway("Exception while fetching organizations: " + e.getMessage());
        }
    }

    public String listContacts(int offset) {
        String url = normalizedBaseUrl() + "/contacts/?limit=" + pageSize + "&offset=" + offset;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Token " + token)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpStatus.OK.value()) {
                return response.body();
            } else if (response.statusCode() == 429) {
                throw new RateLimitException(60);
            } else {
                log.error("Error fetching contacts: HTTP {} - {}", response.statusCode(), response.body());
                throw new ApiException(HttpStatus.valueOf(response.statusCode()), response.body());
            }
        } catch (ApiException | RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Exception while fetching contacts", e);
            throw ApiException.badGateway("Exception while fetching contacts: " + e.getMessage());
        }
    }

    public String fetchOrganizationById(String organizationId) {
        String url = normalizedBaseUrl() + "/organizations/" + organizationId + "/";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Token " + token)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpStatus.OK.value()) {
                return response.body();
            } else if (response.statusCode() == 404) {
                throw ApiException.notFound("Organization not found: " + organizationId);
            } else if (response.statusCode() == 429) {
                throw new RateLimitException(60);
            } else {
                log.error("Error fetching organization by ID: HTTP {} - {}", response.statusCode(), response.body());
                throw new ApiException(HttpStatus.valueOf(response.statusCode()), response.body());
            }
        } catch (ApiException | RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("Exception while fetching organization by ID", e);
            throw ApiException.badGateway("Exception while fetching organization by ID: " + e.getMessage());
        }
    }

    public ClientTarget previewClientMigrated(String organizationId) {
        String organizationJson = fetchOrganizationById(organizationId);
        try {
            ClientSource source = objectMapper.readValue(organizationJson, ClientSource.class);
            return clientMapper.toTarget(source);
        } catch (Exception e) {
            log.error("Error parsing organization JSON for ID {}", organizationId, e);
            throw ApiException.badGateway("Error parsing organization JSON for ID " + organizationId);
        }
    }

    private String normalizedBaseUrl() {
        return baseUrl.replaceAll("/+$", ""); // Remove trailing slashes
    }
}