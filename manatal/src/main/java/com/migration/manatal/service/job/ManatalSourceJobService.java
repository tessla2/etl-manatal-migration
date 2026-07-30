package com.migration.manatal.service.job;


import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManatalSourceJobService {

    private final HttpClient httpClient;

    @Value("${migration.manatal.base-url}")
    private String baseUrl;

    @Value("${migration.manatal.source.token}")
    private String token;

    @Value("${migration.manatal.page-size:100}")
    private int pageSize;

    @Value("${migration.manatal.rate-limit-retry-seconds:60}")
    private int rateLimitRetrySeconds;

    //============================== Jobs =================================

    public String listJobs(int offset) {
        String url = normalizedBaseUrl() + "/jobs/?limit=" + pageSize + "&offset=" + offset;
        return sendGetRequest(url, "fetching Jobs");
    }


    private String sendGetRequest(String url, String context) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Token " + token)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HttpStatus.OK.value()) {
                return response.body();
            }

            if (response.statusCode() == 429) {
                throw new RateLimitException(rateLimitRetrySeconds);
            }

            log.error("Error {}: HTTP {} - {}", context, response.statusCode(), response.body());
            throw new ApiException(HttpStatus.valueOf(response.statusCode()), response.body());

        } catch (ApiException | RateLimitException e) {
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
