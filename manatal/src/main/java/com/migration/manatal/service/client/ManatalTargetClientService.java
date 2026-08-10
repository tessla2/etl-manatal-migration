package com.migration.manatal.service.client;

import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.service.ManatalApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ManatalTargetClientService {

    private final ManatalApiClient apiClient;

    @Value("${migration.manatal.target.token}")
    private String targetToken;

    public String migrateOrganization(ClientTarget target) {
        String url = apiClient.endpoint("/organizations/");
        return apiClient.post(url, target, targetToken, "migrating organization");
    }

    public String createOrganizationNote(int organizationId, String content) {
        String url = apiClient.endpoint("/organizations/" + organizationId + "/notes/");
        return apiClient.post(url, Map.of("info", content), targetToken, "creating note for organization " + organizationId);
    }

    public String createContactNote(long contactId, String content) {
        String url = apiClient.endpoint("/contacts/" + contactId + "/notes/");
        return apiClient.post(url, Map.of("info", content), targetToken, "creating note for contact " + contactId);
    }

    public String createContact(ClientTarget.ContactTarget contact) {
        String url = apiClient.endpoint("/contacts/");
        return apiClient.post(url, contact, targetToken, "creating contact");
    }
}
