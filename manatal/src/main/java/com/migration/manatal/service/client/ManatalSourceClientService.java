package com.migration.manatal.service.client;

import com.migration.manatal.config.OwnerMappingProperties;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.NonRetryableApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.client.ClientSource;
import com.migration.manatal.model.client.ClientSource.SourceContact;
import com.migration.manatal.model.client.ClientSource.SourceNote;
import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.service.ManatalApiClient;
import com.migration.manatal.transform.ClientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManatalSourceClientService {

    private final ObjectMapper objectMapper;
    private final ClientMapper clientMapper;
    private final ManatalApiClient apiClient;
    private final OwnerMappingProperties ownerMappingProperties;

    @Value("${migration.manatal.source.token}")
    private String token;

    @Value("${migration.manatal.page-size:100}")
    private int pageSize;

    //============================== Organizations =================================

    public String listOrganizations(int offset) {
        String url = apiClient.endpoint("/organizations/?limit=" + pageSize + "&offset=" + offset);
        return apiClient.get(url, token, "fetching organizations");
    }

    public String fetchOrganizationById(String organizationId) {
        String url = apiClient.endpoint("/organizations/" + organizationId + "/");
        try {
            return apiClient.get(url, token, "fetching organization by ID");
        } catch (ApiException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                throw new NonRetryableApiException(HttpStatus.NOT_FOUND,
                        "Organization not found: " + organizationId);
            }
            throw e;
        }
    }

    public void updateCustomField(String organizationId, String field, String value) {
        String url = apiClient.endpoint("/organizations/" + organizationId + "/");
        Map<String, Object> customFields = new HashMap<>(fetchCustomFields(organizationId));
        customFields.put(field, value);
        Map<String, Object> body = Map.of("custom_fields", customFields);
        apiClient.patch(url, body, token, "updating custom field for organization " + organizationId);
    }

    private Map<String, Object> fetchCustomFields(String organizationId) {
        try {
            JsonNode root = objectMapper.readTree(fetchOrganizationById(organizationId));
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
            log.error("Error reading custom fields for organization {}", organizationId, e);
            throw ApiException.badGateway("Error reading custom fields for organization " + organizationId, e);
        }
    }

    public String listOrganizationsPage(int page) {
        String url = apiClient.endpoint("/organizations/?page=" + page + "&page_size=" + pageSize);
        return apiClient.get(url, token, "fetching organizations page " + page);
    }

    public List<OrganizationExportInfo> listOrganizationsWithExportFilter() {
        List<OrganizationExportInfo> result = new java.util.ArrayList<>();
        int page = 1;
        try {
            while (true) {
                String json = listOrganizationsPage(page);
                var root = objectMapper.readTree(json);
                var results = root.path("results");

                if (!results.isArray() || results.isEmpty()) {
                    log.info("Organizations page {} is empty, stopping", page);
                    break;
                }

                log.info("Organizations page {} returned {} org(s)", page, results.size());
                for (var org : results) {
                    if ("To Export".equals(org.path("custom_fields").path("exported").asString(""))) {
                        result.add(new OrganizationExportInfo(
                                org.path("id").asString(),
                                org.path("name").asString("")));
                    }
                }

                if (results.size() < pageSize) {
                    break;
                }
                page++;
            }
            log.info("Organizations with exported='To Export': {} found", result.size());
            return result;
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error listing organizations with export filter", e);
            throw ApiException.badGateway("Error listing organizations with export filter", e);
        }
    }

    public record OrganizationExportInfo(String id, String name) {}

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

            List<SourceContact> contactSources = listAllContactsByOrganization(organizationIdNumber);
            log.info("Organization {}: {} contact(s) found", organizationId, contactSources.size());

            List<SourceNote> noteSources = listNotesByContacts(contactSources);
            int contactNotesCount = noteSources.size();
            noteSources.addAll(listOrganizationNotesBestEffort(organizationId));
            log.info("Organization {}: {} contact note(s) + {} organization note(s) = {} note(s)",
                    organizationId, contactNotesCount, noteSources.size() - contactNotesCount, noteSources.size());

            Map<Integer, String> userNames = listUsersBestEffort();
            Map<Integer, String> creatorNameFallback = ownerMappingProperties.getCreatorNameMapping();
            int unnamedNotes = 0;
            int fromUsers = 0;
            int fromFallback = 0;
            for (SourceNote note : noteSources) {
                if (note.getCreator() != null) {
                    String name = userNames.get(note.getCreator());
                    if (name == null || name.isBlank()) {
                        name = creatorNameFallback.get(note.getCreator());
                        if (name != null && !name.isBlank()) {
                            fromFallback++;
                        }
                    } else {
                        fromUsers++;
                    }
                    if (name != null && !name.isBlank()) {
                        note.setCreatorName(name);
                    } else {
                        unnamedNotes++;
                    }
                } else {
                    unnamedNotes++;
                }
            }
            if (unnamedNotes > 0) {
                var unnamedCreatorIds = noteSources.stream()
                        .filter(n -> n.getCreatorName() == null || n.getCreatorName().isBlank())
                        .map(SourceNote::getCreator)
                        .collect(java.util.stream.Collectors.toSet());
                log.warn("Organization {}: {} of {} note(s) have no resolvable creator name "
                        + "(creator null or not in /users/ and no creator-name-mapping). Creator id(s): {}",
                        organizationId, unnamedNotes, noteSources.size(), unnamedCreatorIds);
            } else {
                log.info("Organization {}: all {} note(s) have creator name ({} via /users/, {} via creator-name-mapping)",
                        organizationId, noteSources.size(), fromUsers, fromFallback);
            }

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
        String url = apiClient.endpoint("/contacts/?limit=" + pageSize + "&offset=" + offset);
        return apiClient.get(url, token, "fetching contacts");
    }

    public String listContactsByOrganizationPage(int organizationId, int page) {
        String url = apiClient.endpoint("/contacts/?page=" + page + "&page_size=" + pageSize + "&organization_id=" + organizationId);
        return apiClient.get(url, token, "fetching contacts page " + page + " for organization " + organizationId);
    }

    public List<SourceContact> listAllContactsByOrganization(int organizationId) {
        List<SourceContact> contacts = new java.util.ArrayList<>();
        int page = 1;
        try {
            while (true) {
                String json = listContactsByOrganizationPage(organizationId, page);
                var results = objectMapper.readTree(json).path("results");

                if (!results.isArray() || results.isEmpty()) {
                    log.info("Organization {}: contacts page {} is empty, stopping ({} contact(s) so far)",
                            organizationId, page, contacts.size());
                    break;
                }

                List<SourceContact> pageContacts = apiClient.parseResultsList(json, SourceContact.class);
                contacts.addAll(pageContacts);
                log.info("Organization {}: contacts page {} returned {} contact(s) (total so far: {})",
                        organizationId, page, pageContacts.size(), contacts.size());

                if (results.size() < pageSize) {
                    break;
                }
                page++;
            }
            return contacts;
        } catch (RateLimitException e) {
            throw e;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error listing contacts for organization {}", organizationId, e);
            throw ApiException.badGateway("Error listing contacts for organization " + organizationId, e);
        }
    }

    // ============================= Notes =============================

    public String listContactNotes(Integer contactId, int offset) {
        String url = apiClient.endpoint("/contacts/" + contactId + "/notes/?limit=" + pageSize + "&offset=" + offset);
        return apiClient.get(url, token, "fetching notes for contact " + contactId);
    }

    public List<SourceNote> listNotesByContacts(List<SourceContact> contacts) throws Exception {
        List<SourceNote> notes = new java.util.ArrayList<>();
        for (SourceContact contact : contacts) {
            if (contact.getId() == null) {
                continue;
            }
            String notesJson = listContactNotes(contact.getId(), 0);
            List<SourceNote> contactNotes = apiClient.parseResultsList(notesJson, SourceNote.class);
            for (SourceNote note : contactNotes) {
                note.setContactId(contact.getId());
            }
            notes.addAll(contactNotes);
        }
        return notes;
    }

    public List<SourceNote> listOrganizationNotesBestEffort(String organizationId) {
        try {
            String notesJson = listOrganizationNotes(organizationId, 0);
            return apiClient.parseResultsList(notesJson, SourceNote.class);
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Organization notes unavailable for organization {} (best-effort, continuing): {}", organizationId, e.getMessage());
            return List.of();
        }
    }

    public String listOrganizationNotes(String organizationId, int offset) {
        String url = apiClient.endpoint("/organizations/" + organizationId + "/notes/?limit=" + pageSize + "&offset=" + offset);
        return apiClient.get(url, token, "fetching notes for organization " + organizationId);
    }

    // ============================== Users ==============================

    public Map<Integer, String> listUsersBestEffort() {
        try {
            Map<Integer, String> users = new java.util.LinkedHashMap<>();
            int page = 1;
            while (true) {
                String url = apiClient.endpoint("/users/?page=" + page + "&page_size=" + pageSize);
                String json = apiClient.get(url, token, "fetching users page " + page);
                var results = objectMapper.readTree(json).path("results");

                if (!results.isArray() || results.isEmpty()) {
                    break;
                }

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

                if (results.size() < pageSize) {
                    break;
                }
                page++;
            }
            return users;
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Users unavailable for creator-name resolution (best-effort, continuing): {}", e.getMessage());
            return new java.util.LinkedHashMap<>();
        }
    }

}
