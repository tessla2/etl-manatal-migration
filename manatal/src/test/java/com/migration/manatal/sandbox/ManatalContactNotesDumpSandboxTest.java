package com.migration.manatal.sandbox;

import com.migration.manatal.service.ManatalApiClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Slf4j
class ManatalContactNotesDumpSandboxTest {

    private static final String BASE_URL = System.getenv().getOrDefault("MANATAL_BASE_URL", "https://api.manatal.com/open/v3/");
    private static final String TOKEN = System.getenv().getOrDefault("MANATAL_TOKEN",
            System.getenv().getOrDefault("MANATAL_TARGET_TOKEN", ""));
    private static final String ORGANIZATION_ID = System.getenv().getOrDefault("MANATAL_ORGANIZATION_ID", "");
    private static final String CONTACT_ID = System.getenv().getOrDefault("MANATAL_CONTACT_ID", "");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ManatalApiClient apiClient;

    @BeforeEach
    void setUp() throws Exception {
        assumeTrue(!TOKEN.isBlank(), "Skipped: set MANATAL_TOKEN (source) to inspect contact notes");
        assumeTrue(!ORGANIZATION_ID.isBlank(), "Skipped: set MANATAL_ORGANIZATION_ID (and optionally MANATAL_CONTACT_ID)");

        apiClient = new ManatalApiClient(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(), objectMapper);

        var baseUrlField = ManatalApiClient.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(apiClient, BASE_URL);

        var retrySecondsField = ManatalApiClient.class.getDeclaredField("rateLimitRetrySeconds");
        retrySecondsField.setAccessible(true);
        retrySecondsField.set(apiClient, 1);

        var retryLimitField = ManatalApiClient.class.getDeclaredField("retryLimit");
        retryLimitField.setAccessible(true);
        retryLimitField.set(apiClient, 3);
    }

    @Test
    void shouldDumpContactNotesRawJson() throws Exception {
        long orgId = Long.parseLong(ORGANIZATION_ID.trim());

        log.info("=== GET /organizations/{}/ (detail) ===", orgId);
        try {
            String orgJson = apiClient.get(
                    apiClient.endpoint("/organizations/" + orgId + "/"), TOKEN, "fetching organization " + orgId);
            JsonNode org = objectMapper.readTree(orgJson);
            log.info("name={}, address={}", org.path("name").asString(""), org.path("address").asString(""));
        } catch (Exception e) {
            log.info("GET organization {} -> ERROR: {}", orgId, e.getMessage());
        }

        log.info("=== GET /users/ (creator-name cross-reference) ===");
        try {
            String usersJson = apiClient.get(
                    apiClient.endpoint("/users/?page=1&page_size=100"), TOKEN, "fetching users");
            JsonNode results = objectMapper.readTree(usersJson).path("results");
            if (!results.isArray()) {
                log.info("users response is NOT {results:[]}: {}", usersJson);
            } else {
                for (JsonNode user : results) {
                    log.info("user id={} display_name='{}' full_name='{}' email='{}'",
                            user.path("id").asInt(0),
                            user.path("display_name").asString(""),
                            user.path("full_name").asString(""),
                            user.path("email").asString(""));
                }
            }
        } catch (Exception e) {
            log.info("GET /users/ -> ERROR: {}", e.getMessage());
        }

        if (!CONTACT_ID.isBlank()) {
            dumpContactNotes(Long.parseLong(CONTACT_ID.trim()));
            return;
        }

        log.info("=== GET /contacts/?organization_id={} (all pages) ===", orgId);
        int page = 1;
        while (true) {
            String contactsJson = apiClient.get(
                    apiClient.endpoint("/contacts/?page=" + page + "&page_size=100&organization_id=" + orgId),
                    TOKEN, "fetching contacts page " + page);
            JsonNode results = objectMapper.readTree(contactsJson).path("results");
            if (!results.isArray() || results.isEmpty()) {
                break;
            }
            for (JsonNode contact : results) {
                long contactId = contact.path("id").asLong(0);
                if (contactId == 0) {
                    continue;
                }
                log.info("contact id={} full_name='{}'", contactId, contact.path("full_name").asString(""));
                dumpContactNotes(contactId);
            }
            if (results.size() < 100) {
                break;
            }
            page++;
        }
    }

    private void dumpContactNotes(long contactId) {
        try {
            String json = apiClient.get(
                    apiClient.endpoint("/contacts/" + contactId + "/notes/"),
                    TOKEN, "fetching notes for contact " + contactId);
            log.info("=== GET /contacts/{}/notes/ (RAW) ===", contactId);
            log.info(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(json)));
            JsonNode arr = objectMapper.readTree(json);
            if (arr.isArray()) {
                for (JsonNode note : arr) {
                    log.info("note id={} info='{}' creator={} (creator present={})",
                            note.path("id").asLong(0),
                            note.path("info").asString("").length() > 60
                                    ? note.path("info").asString("").substring(0, 60) + "..."
                                    : note.path("info").asString(""),
                            note.path("creator"),
                            note.hasNonNull("creator"));
                }
            }
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            log.info("=== GET /contacts/{}/notes/ -> ERROR: {} ===", contactId, msg);
        }
    }
}
