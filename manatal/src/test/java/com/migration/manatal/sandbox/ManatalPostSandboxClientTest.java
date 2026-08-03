package com.migration.manatal.sandbox;

import com.migration.manatal.model.client.ClientSource;
import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.service.client.ManatalTargetClientService;
import com.migration.manatal.transform.ClientMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@EnabledIfEnvironmentVariable(named = "MANATAL_TARGET_TOKEN", matches = ".+")
class ManatalPostSandboxClientTest {

    private static final String BASE_URL = System.getenv().getOrDefault("MANATAL_BASE_URL", "https://api.manatal.com/open/v3/");

    private static final String SAMPLE_SOURCE_CLIENT = """
            {
              "name": "Acme Mock Corp",
              "website": "https://acme.example.com",
              "logo": "https://acme.example.com/logo.png",
              "address": "Porto, Portugal",
              "description": "Cliente mock para validar a estrutura do POST de organizacoes.",
              "owner": 123,
              "custom_fields": {
                "clientbusinessarea": ["IT", "R&S"]
              }
            }
            """;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ClientMapper clientMapper = new ClientMapper();
    private ManatalTargetClientService targetService;

    @BeforeEach
    void setUp() throws Exception {
        targetService = new ManatalTargetClientService(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build(), objectMapper);

        var baseUrlField = ManatalTargetClientService.class.getDeclaredField("baseUrl");
        baseUrlField.setAccessible(true);
        baseUrlField.set(targetService, BASE_URL);

        var tokenField = ManatalTargetClientService.class.getDeclaredField("targetToken");
        tokenField.setAccessible(true);
        tokenField.set(targetService, System.getenv("MANATAL_TARGET_TOKEN"));

        var retrySecondsField = ManatalTargetClientService.class.getDeclaredField("rateLimitRetrySeconds");
        retrySecondsField.setAccessible(true);
        retrySecondsField.set(targetService, 1);

        var retryLimitField = ManatalTargetClientService.class.getDeclaredField("retryLimit");
        retryLimitField.setAccessible(true);
        retryLimitField.set(targetService, 3);
    }

    @Test
    void shouldPostMockClientWithContactsAndNotesToTarget() throws Exception {
        ClientSource source = objectMapper.readValue(SAMPLE_SOURCE_CLIENT, ClientSource.class);

        var contact1 = new ClientSource.SourceContact();
        contact1.setFullName("Maria Silva");
        contact1.setDisplayName("Maria");
        contact1.setEmail("maria@acme.example.com");
        contact1.setPhoneNumber("+351 910 000 000");
        contact1.setDescription("Contacto principal");

        var contact2 = new ClientSource.SourceContact();
        contact2.setFullName("Joao Pereira");
        contact2.setDisplayName("Joao");
        contact2.setEmail("joao@acme.example.com");
        contact2.setPhoneNumber("+351 920 000 000");
        contact2.setDescription("Contacto financeiro");

        var note = new ClientSource.SourceNote();
        note.setContent("Nota de teste: cliente contactado no dia 30/07 para validar o POST de notas.");
        note.setCreator(1);
        note.setCreatedAt("2026-07-30T14:00:00Z");

        ClientTarget target = clientMapper.toTarget(source, List.of(contact1, contact2), List.of(note));
        target.setClientName("[MIGRATION-TEST] " + target.getClientName());
        target.setClientIndustry(null);
        target.setCustomFields(Map.of("clientbusinessarea", List.of("IT", "R&S")));

        List<ClientTarget.ContactTarget> contacts = target.getContacts();
        List<ClientTarget.TargetNote> notes = target.getNotes();
        target.setContacts(null);
        target.setNotes(null);

        log.info("=== PAYLOAD SENT TO POST /organizations/ (contacts/notes stripped) ===");
        log.info(objectMapper.writeValueAsString(target));

        String response = targetService.migrateOrganization(target);
        log.info("=== RESPONSE POST /organizations/ ===");
        log.info(response);

        long targetOrgId = objectMapper.readTree(response).path("id").asLong();
        assertTrue(targetOrgId > 0, "Expected a target organization id in response: " + response);

        for (ClientTarget.ContactTarget contact : contacts) {
            contact.setOrganization(targetOrgId);
            log.info("=== POST /contacts/ -> {} ===", contact.getFullName());
            String contactResponse = targetService.createContact(contact);
            log.info(contactResponse);
            long contactId = objectMapper.readTree(contactResponse).path("id").asLong();
            assertTrue(contactId > 0, "Expected a target contact id in response: " + contactResponse);
        }

        for (ClientTarget.TargetNote noteContent : notes) {
            log.info("=== POST /organizations/{}/notes/ ===", targetOrgId);
            String noteResponse = targetService.createOrganizationNote((int) targetOrgId, noteContent.getContent());
            log.info(noteResponse);
            long noteId = objectMapper.readTree(noteResponse).path("id").asLong();
            assertTrue(noteId > 0, "Expected a target note id in response: " + noteResponse);
        }
    }
}
