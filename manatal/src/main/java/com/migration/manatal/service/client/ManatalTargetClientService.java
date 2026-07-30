package com.migration.manatal.service.client;

import com.migration.manatal.exception.ApiException;
import com.migration.manatal.model.client.ClientTarget;
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
@RequiredArgsConstructor
@Service
public class ManatalTargetClientService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${migration.manatal.base-url}")
    private String baseUrl;

    @Value("${migration.manatal.target.token}")
    private String targetToken;

    public String migrateOrganization(ClientTarget target) {
        String url = normalizedBaseUrl() + "/organizations/";
        return sendPostRequest(url, target, "migrating organization");
    }

    //============================== HTTP  =================================
    private String sendPostRequest(String url, Object body, String context) {
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

            log.error("Error {}: HTTP {} - {}", context, response.statusCode(), response.body());
            throw new ApiException(HttpStatus.valueOf(response.statusCode()), response.body());

        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Exception while {}", context, e);
            throw ApiException.badGateway("Exception while " + context, e);
        }
    }

    private String normalizedBaseUrl() {
        return baseUrl.replaceAll("/+$", "");
    }
}
