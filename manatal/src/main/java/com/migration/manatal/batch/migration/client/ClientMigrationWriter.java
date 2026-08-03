package com.migration.manatal.batch.migration.client;

import com.migration.manatal.entity.client.ClientMigration;
import com.migration.manatal.exception.ApiException;
import com.migration.manatal.exception.RateLimitException;
import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.repository.client.ClientMigrationRepository;
import com.migration.manatal.service.client.ManatalSourceClientService;
import com.migration.manatal.service.client.ManatalTargetClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientMigrationWriter implements ItemWriter<ClientMigrationPackage> {

    private final ManatalTargetClientService targetService;
    private final ManatalSourceClientService sourceService;
    private final ClientMigrationRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    public void write(Chunk<? extends ClientMigrationPackage> chunk) throws Exception {
        for (ClientMigrationPackage pkg : chunk) {
            ClientMigration entity = pkg.getEntity();

            try {
                if (pkg.getErrorMessage() != null) {
                    markErro(entity, pkg.getErrorMessage());
                    continue;
                }

                ClientTarget transformed = pkg.getTransformed();
                List<ClientTarget.ContactTarget> contacts = transformed.getContacts() == null ? List.of() : transformed.getContacts();
                List<ClientTarget.TargetNote> notes = transformed.getNotes() == null ? List.of() : transformed.getNotes();
                transformed.setContacts(null);
                transformed.setNotes(null);

                String response = targetService.migrateOrganization(transformed);
                long targetOrganizationId;
                try {
                    targetOrganizationId = objectMapper.readTree(response).path("id").asLong();
                } catch (Exception e) {
                    markErro(entity, "Organization created but failed to parse target id: " + e.getMessage());
                    continue;
                }

                entity.setTargetOrganizationId(targetOrganizationId);
                entity.setStatus("SUCESSO");
                entity.setErrorMessage(null);
                repository.save(entity);

                try {
                    sourceService.updateCustomField(entity.getSourceOrganizationId(), "exported", "Yes");
                    entity.setTaggedInSource(true);
                    repository.save(entity);
                    log.info("Client {} migrated successfully and marked as exported", entity.getSourceOrganizationId());
                } catch (Exception e) {
                    log.warn("Client {} migrated but failed to mark exported: {}", entity.getSourceOrganizationId(), e.getMessage());
                }

                postContacts(targetOrganizationId, contacts, entity);
                postNotes(targetOrganizationId, notes, entity);
            } catch (RateLimitException e) {
                throw e;
            } catch (ApiException e) {
                if (e.isRetryable()) {
                    throw e;
                }
                markErro(entity, e.getMessage());
            } catch (Exception e) {
                markErro(entity, e.getMessage());
            }
        }
    }

    private void postContacts(long targetOrganizationId, List<ClientTarget.ContactTarget> contacts, ClientMigration entity) {
        if (contacts.isEmpty()) {
            return;
        }
        int posted = 0;
        for (ClientTarget.ContactTarget contact : contacts) {
            if (contact.getFullName() == null || contact.getFullName().isBlank()) {
                continue;
            }
            contact.setOrganization(targetOrganizationId);
            try {
                targetService.createContact(contact);
                posted++;
            } catch (Exception e) {
                log.warn("Client {}: failed to create contact '{}': {}",
                        entity.getSourceOrganizationId(), contact.getFullName(), e.getMessage());
            }
        }
        log.info("Client {}: created {}/{} contacts for target organization {}",
                entity.getSourceOrganizationId(), posted, contacts.size(), targetOrganizationId);
    }

    private void postNotes(long targetOrganizationId, List<ClientTarget.TargetNote> notes, ClientMigration entity) {
        if (notes.isEmpty()) {
            return;
        }
        int posted = 0;
        for (ClientTarget.TargetNote note : notes) {
            if (note.getContent() == null || note.getContent().isBlank()) {
                continue;
            }
            try {
                targetService.createOrganizationNote((int) targetOrganizationId, note.getContent());
                posted++;
            } catch (Exception e) {
                log.warn("Client {}: failed to create note '{}...': {}",
                        entity.getSourceOrganizationId(), preview(note.getContent()), e.getMessage());
            }
        }
        log.info("Client {}: created {}/{} notes for target organization {}",
                entity.getSourceOrganizationId(), posted, notes.size(), targetOrganizationId);
    }

    private String preview(String content) {
        if (content == null) {
            return "";
        }
        return content.length() > 50 ? content.substring(0, 50) + "..." : content;
    }

    private void markErro(ClientMigration entity, String message) {
        entity.setStatus("ERRO");
        entity.setErrorMessage(message);
        repository.save(entity);
        log.error("Client {} marked as ERRO due to: {}", entity.getSourceOrganizationId(), message);
    }
}
