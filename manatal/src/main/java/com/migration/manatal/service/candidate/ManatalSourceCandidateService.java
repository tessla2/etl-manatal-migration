package com.migration.manatal.service.candidate;


import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.NonRetryableApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.candidate.CandidateSource;
import com.migration.manatal.model.candidate.CandidateTarget;
import com.migration.manatal.service.ManatalApiClient;
import com.migration.manatal.transform.CandidateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManatalSourceCandidateService {

    private final ObjectMapper objectMapper;
    private final CandidateMapper candidateMapper;
    private final ManatalApiClient apiClient;

    @Value("${migration.manatal.source.token}")
    private String token;

    @Value("${migration.manatal.page-size:100}")
    private int pageSize;

    // ============================== Candidates ==============================

    public String listCandidates(int offset) {
        String url = apiClient.endpoint("/candidates/?limit=" + pageSize + "&offset=" + offset);
        return apiClient.get(url, token, "fetching candidates");
    }

    public String listCandidatesPage(int page) {
        String url = apiClient.endpoint("/candidates/?page=" + page + "&page_size=" + pageSize);
        return apiClient.get(url, token, "fetching candidates page " + page);
    }

    private static final String TO_EXPORT_TAG = "To Export";

    public List<CandidateExportInfo> listCandidatesWithExportFilter() {
        List<CandidateExportInfo> result = new ArrayList<>();
        int page = 1;
        try {
        while (true) {
            String json = listCandidatesPageByTag(page);
            var root = objectMapper.readTree(json);
            var results = root.path("results");

            if (!results.isArray() || results.isEmpty()) {
                log.info("Candidates loader: page {} is empty, stopping ({} candidate(s) with tag '{}' so far)",
                        page, result.size(), TO_EXPORT_TAG);
                break;
            }

            int matched = 0;
            for (var candidate : results) {
                if (hasExportTag(candidate)) {
                    result.add(new CandidateExportInfo(
                            candidate.path("id").asString(),
                            candidate.path("full_name").asString("")));
                    matched++;
                }
            }
            log.info("Candidates loader: page {} returned {} candidate(s), {} with tag '{}' (total so far: {})",
                    page, results.size(), matched, TO_EXPORT_TAG, result.size());

            if (results.size() < pageSize) {
                log.info("Candidates loader: page {} is partial ({} < {}), stopping", page, results.size(), pageSize);
                break;
            }
            page++;
        }
            log.info("Candidates loader: complete, {} candidate(s) with tag '{}'", result.size(), TO_EXPORT_TAG);
            return result;
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error listing candidates with tag '{}'", TO_EXPORT_TAG, e);
            throw ApiException.badGateway("Error listing candidates with tag '" + TO_EXPORT_TAG + "'", e);
        }
    }

    private String listCandidatesPageByTag(int page) {
        String encodedTag = URLEncoder.encode(TO_EXPORT_TAG, StandardCharsets.UTF_8).replace("+", "%20");
        String url = apiClient.endpoint("/candidates/?page=" + page + "&page_size=" + pageSize
                + "&candidate_tags=" + encodedTag);
        return apiClient.get(url, token, "fetching candidates page " + page + " tagged '" + TO_EXPORT_TAG + "'");
    }

    private boolean hasExportTag(JsonNode candidate) {
        JsonNode tags = candidate.path("candidate_tags");
        if (!tags.isArray()) {
            return false;
        }
        for (JsonNode tag : tags) {
            String name = tag.path("tag_name").asString("");
            if (name.isBlank()) {
                name = tag.path("tag").asString("");
            }
            if (TO_EXPORT_TAG.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public record CandidateExportInfo(String id, String fullName) {}

    public String fetchCandidateById(String candidateId) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/");
        try {
            return apiClient.get(url, token, "fetching candidate by ID");
        } catch (ApiException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                throw new NonRetryableApiException(HttpStatus.NOT_FOUND,
                        "Candidate not found: " + candidateId);
            }
            throw e;
        }
    }

    public void updateCustomField(String sourceCandidateId, String field, String value) {
        String url = apiClient.endpoint("/candidates/" + sourceCandidateId + "/");
        Map<String, Object> customFields = new HashMap<>(fetchCustomFields(sourceCandidateId));
        customFields.put(field, value);
        Map<String, Object> body = Map.of("custom_fields", customFields);
        apiClient.patch(url, body, token, "updating custom field for candidate " + sourceCandidateId);
    }

    public void addCandidateTag(String sourceCandidateId, String tagName) {
        String url = apiClient.endpoint("/candidates/" + sourceCandidateId + "/tags/");
        apiClient.post(url, Map.of("tag", tagName), token,
                "adding tag '" + tagName + "' to candidate " + sourceCandidateId);
    }

    public Long getCandidateTagId(String sourceCandidateId, String tagName) {
        try {
            JsonNode root = objectMapper.readTree(fetchCandidateById(sourceCandidateId));
            JsonNode tags = root.path("candidate_tags");
            if (!tags.isArray()) {
                return null;
            }
            for (JsonNode tag : tags) {
                String name = tag.path("tag_name").asString("");
                if (name.isBlank()) {
                    name = tag.path("tag").asString("");
                }
                if (tagName.equals(name)) {
                    long id = tag.path("id").asLong(0);
                    return id == 0 ? null : id;
                }
            }
            return null;
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error finding tag '{}' for candidate {}", tagName, sourceCandidateId, e);
            throw ApiException.badGateway("Error finding tag '" + tagName + "' for candidate " + sourceCandidateId, e);
        }
    }

    public void removeCandidateTag(String sourceCandidateId, Long tagId) {
        String url = apiClient.endpoint("/candidates/" + sourceCandidateId + "/tags/" + tagId + "/");
        apiClient.delete(url, token, "removing tag " + tagId + " from candidate " + sourceCandidateId);
    }

    private Map<String, Object> fetchCustomFields(String sourceCandidateId) {
        try {
            JsonNode root = objectMapper.readTree(fetchCandidateById(sourceCandidateId));
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
            log.error("Error reading custom fields for candidate {}", sourceCandidateId, e);
            throw ApiException.badGateway("Error reading custom fields for candidate " + sourceCandidateId, e);
        }
    }

    public CandidateTarget previewCandidateMigrated(String candidateId) {
        String json = fetchCandidateById(candidateId);
        try {
            CandidateSource source = objectMapper.readValue(json, CandidateSource.class);
            List<CandidateSource.CandidateNote> notes = getCandidateNotes(candidateId);
            List<CandidateSource.Nationality> nationalities = getCandidateNationalities(candidateId);
            return candidateMapper.toTarget(source, notes, nationalities, source.getSkills());
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error processing candidate {} for preview", candidateId, e);
            throw ApiException.badGateway("Error processing candidate " + candidateId + " for preview", e);
        }
    }

    // ============================== Sub-resources ==============================

    public List<CandidateSource.CandidateNote> getCandidateNotes(String candidateId) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/notes/");
        try {
            String json = apiClient.get(url, token, "fetching notes for candidate " + candidateId);
            return apiClient.parseResultsList(json, CandidateSource.CandidateNote.class);
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error listing notes for candidate {}", candidateId, e);
            throw ApiException.badGateway("Error listing notes for candidate " + candidateId, e);
        }
    }

    public List<CandidateSource.Nationality> getCandidateNationalities(String candidateId) {
        try {
            return pageList("/candidates/" + candidateId + "/nationalities/",
                    CandidateSource.Nationality.class, "nationalities for candidate " + candidateId);
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error listing nationalities for candidate {}", candidateId, e);
            throw ApiException.badGateway("Error listing nationalities for candidate " + candidateId, e);
        }
    }

    public List<CandidateSource.CandidateMatch> getCandidateMatches(String candidateId) {
        try {
            return pageList("/candidates/" + candidateId + "/matches/",
                    CandidateSource.CandidateMatch.class, "matches for candidate " + candidateId);
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error listing matches for candidate {}", candidateId, e);
            throw ApiException.badGateway("Error listing matches for candidate " + candidateId, e);
        }
    }

    public List<CandidateSource.Activity> getCandidateActivities(String candidateId) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/activities/");
        try {
            String json = apiClient.get(url, token, "fetching activities for candidate " + candidateId);
            return apiClient.parseResultsList(json, CandidateSource.Activity.class);
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error listing activities for candidate {}", candidateId, e);
            throw ApiException.badGateway("Error listing activities for candidate " + candidateId, e);
        }
    }

    public List<CandidateSource.Attachment> getCandidateAttachments(String candidateId) {
        try {
            return pageList("/candidates/" + candidateId + "/attachments/",
                    CandidateSource.Attachment.class, "attachments for candidate " + candidateId);
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error listing attachments for candidate {}", candidateId, e);
            throw ApiException.badGateway("Error listing attachments for candidate " + candidateId, e);
        }
    }

    public CandidateSource.Resume getCandidateResume(String candidateId) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/resume/");
        try {
            String json = apiClient.get(url, token, "fetching resume for candidate " + candidateId);
            return objectMapper.readValue(json, CandidateSource.Resume.class);
        } catch (NonRetryableApiException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw e;
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching resume for candidate {}", candidateId, e);
            throw ApiException.badGateway("Error fetching resume for candidate " + candidateId, e);
        }
    }

    public Map<Integer, String> listUsersBestEffort() {
        try {
            Map<Integer, String> users = new java.util.LinkedHashMap<>();
            int page = 1;
            while (true) {
                String url = apiClient.endpoint("/users/?page=" + page + "&page_size=" + pageSize);
                String json = apiClient.get(url, token, "fetching users page " + page);
                var results = objectMapper.readTree(json).path("results");

                if (!results.isArray() || results.isEmpty()) {
                    log.info("Users loader: page {} is empty, stopping ({} user(s) resolved so far)",
                            page, users.size());
                    break;
                }

                int before = users.size();
                for (var user : results) {
                    int id = user.path("id").asInt(0);
                    if (id == 0) {
                        continue;
                    }
                    String name = user.path("display_name").asString("");
                    if (name.isBlank()) {
                        name = user.path("full_name").asString("");
                    }
                    if (name.isBlank()) {
                        name = user.path("email").asString("");
                    }
                    users.put(id, name);
                }
                log.info("Users loader: page {} returned {} user(s), {} resolvable name(s) (total so far: {})",
                        page, results.size(), users.size() - before, users.size());

                if (results.size() < pageSize) {
                    break;
                }
                page++;
            }
            return users;
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Users unavailable for activity creator resolution (best-effort, continuing): {}",
                    e.getMessage());
            return new java.util.LinkedHashMap<>();
        }
    }

    public List<CandidateSource.SocialMedia> getCandidateSocialMedia(String candidateId) {
        String url = apiClient.endpoint("/candidates/" + candidateId + "/social-media/");
        try {
            String json = apiClient.get(url, token, "fetching social media for candidate " + candidateId);
            return apiClient.parseResultsList(json, CandidateSource.SocialMedia.class);
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error listing social media for candidate {}", candidateId, e);
            throw ApiException.badGateway("Error listing social media for candidate " + candidateId, e);
        }
    }

    // ============================== Helpers ==============================

    private <T> List<T> pageList(String path, Class<T> elementType, String context) throws Exception {
        List<T> all = new ArrayList<>();
        int page = 1;
        while (true) {
            String url = apiClient.endpoint(path + (path.contains("?") ? "&" : "?")
                    + "page=" + page + "&page_size=" + pageSize);
            String json = apiClient.get(url, token, "fetching " + context);
            List<T> items = apiClient.parseResultsList(json, elementType);
            all.addAll(items);
            log.info("{}: page {} returned {} item(s) (total so far: {})", context, page, items.size(), all.size());
            if (items.size() < pageSize) {
                if (!items.isEmpty()) {
                    log.info("{}: page {} is partial ({} < {}), stopping", context, page, items.size(), pageSize);
                }
                break;
            }
            page++;
        }
        return all;
    }
}
