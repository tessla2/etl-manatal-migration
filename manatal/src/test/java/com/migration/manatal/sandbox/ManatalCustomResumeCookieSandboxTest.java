package com.migration.manatal.sandbox;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Slf4j
class ManatalCustomResumeCookieSandboxTest {

    private static final String TOKEN = System.getenv().getOrDefault("MANATAL_TOKEN", "");
    private static final String COOKIE = System.getenv().getOrDefault("MANATAL_COOKIE", "");
    private static final String CANDIDATE_ID = System.getenv().getOrDefault("MANATAL_CANDIDATE_ID", "");

    @Test
    void shouldListCustomResumesWithSessionCookie() throws Exception {
        assumeTrue(!COOKIE.isBlank(), "Skipped: set MANATAL_COOKIE (full Cookie header from the logged-in browser)");
        assumeTrue(!CANDIDATE_ID.isBlank(), "Skipped: set MANATAL_CANDIDATE_ID");

        String base = "https://app.manatal.com/api/v1.0/candidates/" + CANDIDATE_ID.trim();
        probe(base + "/custom-resumes/");
    }

    private void probe(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", COOKIE);
        if (!TOKEN.isBlank()) {
            builder.header("Authorization", "Token " + TOKEN);
        }
        HttpResponse<String> response = client.send(builder.GET().build(), HttpResponse.BodyHandlers.ofString());
        log.info("=== {} -> HTTP {} ===", url, response.statusCode());
        log.info(response.body());
    }
}
