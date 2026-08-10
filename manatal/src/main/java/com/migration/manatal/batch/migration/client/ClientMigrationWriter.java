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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            log.info("Writing client {} ({}) — status={}",
                    entity.getSourceOrganizationId(), entity.getSourceName(), entity.getStatus());

            if (entity.getTargetOrganizationId() != null) {
                log.warn("Client {} already exists in DB with target organization id {} — skipping",
                        entity.getSourceOrganizationId(), entity.getTargetOrganizationId());
                continue;
            }

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
                log.info("Client {}: target organization created with id {}", entity.getSourceOrganizationId(), targetOrganizationId);

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

                Map<Long, Long> sourceToTargetContact = postContacts(targetOrganizationId, contacts, entity);
                postNotes(targetOrganizationId, notes, entity, sourceToTargetContact);
            } catch (RateLimitException e) {                throw e;
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

    private Map<Long, Long> postContacts(long targetOrganizationId, List<ClientTarget.ContactTarget> contacts, ClientMigration entity) {
        Map<Long, Long> sourceToTargetContact = new HashMap<>();
        if (contacts.isEmpty()) {
            return sourceToTargetContact;
        }
        int posted = 0;
        for (ClientTarget.ContactTarget contact : contacts) {
            if (contact.getFullName() == null || contact.getFullName().isBlank()) {
                continue;
            }
            contact.setOrganization(targetOrganizationId);
            try {
                String response = targetService.createContact(contact);
                long targetContactId = 0;
                if (contact.getSourceContactId() != null) {
                    targetContactId = objectMapper.readTree(response).path("id").asLong();
                    if (targetContactId > 0) {
                        sourceToTargetContact.put(contact.getSourceContactId(), targetContactId);
                    }
                }
                posted++;
                log.info("Client {}: contact '{}' created in target (source id {}, target id {})",
                        entity.getSourceOrganizationId(), contact.getFullName(),
                        contact.getSourceContactId(), targetContactId);
            } catch (Exception e) {
                log.warn("Client {}: failed to create contact '{}' (source id {}): {}",
                        entity.getSourceOrganizationId(), contact.getFullName(),
                        contact.getSourceContactId(), e.getMessage());
            }
        }
        log.info("Client {}: created {}/{} contacts for target organization {}",
                entity.getSourceOrganizationId(), posted, contacts.size(), targetOrganizationId);
        return sourceToTargetContact;
    }

    private void postNotes(long targetOrganizationId, List<ClientTarget.TargetNote> notes, ClientMigration entity,
                           Map<Long, Long> sourceToTargetContact) {
        if (notes.isEmpty()) {
            return;
        }
        int posted = 0;
        for (ClientTarget.TargetNote note : notes) {
            if (note.getContent() == null || note.getContent().isBlank()) {
                continue;
            }
            try {
                String content = note.getContent();
                if (note.getCreatorName() != null && !note.getCreatorName().isBlank()) {
                    content = note.getCreatorName() + ": " + content;
                }
                Long targetContactId = note.getContactId() == null ? null : sourceToTargetContact.get(note.getContactId());
                if (targetContactId != null) {
                    targetService.createContactNote(targetContactId, content);
                    log.info("Client {}: note routed to contact {} (author '{}'): '{}...'",
                            entity.getSourceOrganizationId(), targetContactId,
                            note.getCreatorName(), preview(note.getContent()));
                } else {
                    targetService.createOrganizationNote((int) targetOrganizationId, content);
                    log.info("Client {}: note routed to organization {} (author '{}'): '{}...'",
                            entity.getSourceOrganizationId(), targetOrganizationId,
                            note.getCreatorName(), preview(note.getContent()));
                }
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
