package com.migration.manatal.sandbox;

import com.migration.manatal.config.OwnerMappingProperties;
import com.migration.manatal.model.candidate.CandidateSource;
import com.migration.manatal.model.candidate.CandidateTarget;
import com.migration.manatal.service.ManatalApiClient;
import com.migration.manatal.service.candidate.ManatalTargetCandidateService;
import com.migration.manatal.transform.CandidateMapper;
import com.migration.manatal.transform.OwnerMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@EnabledIfEnvironmentVariable(named = "MANATAL_TARGET_TOKEN", matches = ".+")
class ManatalPostSandboxCandidateTest {

    private static final String BASE_URL = System.getenv().getOrDefault("MANATAL_BASE_URL", "https://api.manatal.com/open/v3/");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CandidateMapper candidateMapper = new CandidateMapper(new OwnerMapper(new OwnerMappingProperties()));
    private ManatalTargetCandidateService targetService;

    @BeforeEach
    void setUp() throws Exception {
        ManatalApiClient apiClient = new ManatalApiClient(
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

        targetService = new ManatalTargetCandidateService(apiClient);

        var tokenField = ManatalTargetCandidateService.class.getDeclaredField("targetToken");
        tokenField.setAccessible(true);
        tokenField.set(targetService, System.getenv("MANATAL_TARGET_TOKEN"));
    }

    @Test
    void shouldPostMockCandidateAndSubResourcesToTarget() throws Exception {
        CandidateSource source = new CandidateSource();
        source.setFullName("[MIGRATION-TEST] Candidato Mock");
        source.setCandidateLocation("Lisbon, Portugal");
        source.setEmail("candidato.mock@example.com");
        source.setPhoneNumber("+351 930 000 000");
        source.setDescription("<p>Candidato mock para validar o POST de candidatos.</p>");
        source.setConsent(true);

        CandidateSource.CandidateCustomFields custom = new CandidateSource.CandidateCustomFields();
        custom.setTechnicaldomains("Java, Spring Boot");
        custom.setBusinessdomains(List.of("Chassi"));
        custom.setTechnologies(List.of("Java", "Spring Boot"));
        custom.setTestlifylink("https://app.testlify.com/teste");
        custom.setDocumentaoregularizada(true);
        custom.setPortuguese("C1 (Proficient User)");
        custom.setEnglish(List.of("B1 (Independent User)"));
        custom.setFrench("B2 (Independent User)");
        custom.setSpanish("C2 (Proficient User)");
        custom.setRatehistory("<p>Taxa historica de teste</p>");
        custom.setCandidatecertifications("<p>Certificacao de teste</p>");
        custom.setName("Joao Silva");
        custom.setDate("2026-08-06T23:00:00Z");
        custom.setMaintechnologies(List.of("Spring Boot", "PostgresSQL"));
        source.setCustomFields(custom);

        CandidateSource.Skill skill = new CandidateSource.Skill();
        skill.setSkillName("Java");
        skill.setScore(7);

        CandidateSource.Skill skill2 = new CandidateSource.Skill();
        skill2.setSkillName("Spring Boot");
        skill2.setScore(8);

        CandidateSource.CandidateNote note = new CandidateSource.CandidateNote();
        note.setInfo("<p>Nota de teste do candidato.</p>");
        note.setCreatedAt("2026-08-05T12:00:00Z");

        CandidateSource.Nationality nationality = new CandidateSource.Nationality();
        nationality.setCountry("Portugal");

        CandidateTarget target = candidateMapper.toTarget(source, List.of(note), List.of(nationality), List.of(skill, skill2));

        log.info("=== PAYLOAD SENT TO POST /candidates/ ===");
        log.info(objectMapper.writeValueAsString(target));

        String response = targetService.migrateCandidate(target);
        log.info("=== RESPONSE POST /candidates/ ===");
        log.info(response);

        long targetCandidateId = objectMapper.readTree(response).path("id").asLong();
        assertTrue(targetCandidateId > 0, "Expected a target candidate id in response: " + response);

        log.info("=== PAYLOAD SENT TO POST /candidates/{}/notes/ ===", targetCandidateId);
        log.info("{\"info\": \"Nota de validacao pos-criacao.\"}");

        String noteResponse = targetService.createCandidateNote((int) targetCandidateId, "Nota de validacao pos-criacao.");
        log.info("=== RESPONSE POST note (candidate {}) ===", targetCandidateId);
        log.info(noteResponse);

        log.info("=== PAYLOAD SENT TO POST /candidates/{}/nationalities/ ===", targetCandidateId);
        log.info("{\"country\": \"Brazil\"}");

        String nationalityResponse = targetService.createCandidateNationality((int) targetCandidateId, "Brazil");
        log.info("=== RESPONSE POST nationality (candidate {}) ===", targetCandidateId);
        log.info(nationalityResponse);

        log.info("=== PAYLOAD SENT TO POST /candidates/{}/skills/bulk/ ===", targetCandidateId);
        log.info("{\"skills\": [{\"skill_name\": \"PostgresSQL\", \"score\": 6}, {\"skill_name\": \"Docker\", \"score\": 5}]}");

        String skillsResponse = targetService.addCandidateSkills((int) targetCandidateId, List.of(
                targetSkill("PostgresSQL", 6), targetSkill("Docker", 5)));
        log.info("=== RESPONSE POST skills bulk (candidate {}) ===", targetCandidateId);
        log.info(skillsResponse);
    }

    @Test
    void shouldPostResumeAndAttachmentsToTarget() throws Exception {
        String resumeUrl = System.getenv().getOrDefault("MANATAL_MOCK_RESUME_URL",
                "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf");
        String attachmentUrl = System.getenv().getOrDefault("MANATAL_MOCK_ATTACHMENT_URL",
                "https://pdfobject.com/pdf/sample.pdf");

        CandidateSource source = new CandidateSource();
        source.setFullName("[MIGRATION-TEST] Candidato CV Mock");
        source.setCandidateLocation("Porto, Portugal");
        source.setEmail("candidato.cv.mock@example.com");
        source.setPhoneNumber("+351 930 111 111");
        source.setDescription("<p>Candidato mock para validar CV (resume) e attachments.</p>");
        source.setConsent(true);

        CandidateTarget target = candidateMapper.toTarget(source);

        log.info("=== PAYLOAD SENT TO POST /candidates/ ===");
        log.info(objectMapper.writeValueAsString(target));

        String response = targetService.migrateCandidate(target);
        log.info("=== RESPONSE POST /candidates/ ===");
        log.info(response);

        long targetCandidateId = objectMapper.readTree(response).path("id").asLong();
        assertTrue(targetCandidateId > 0, "Expected a target candidate id in response: " + response);

        log.info("=== PAYLOAD SENT TO POST /candidates/{}/resume/ ===", targetCandidateId);
        log.info("{\"resume_file\": \"" + resumeUrl + "\"}");

        String resumeResponse = targetService.createCandidateResume((int) targetCandidateId, resumeUrl);
        log.info("=== RESPONSE POST resume (candidate {}) ===", targetCandidateId);
        log.info(resumeResponse);

        log.info("=== PAYLOAD SENT TO POST /candidates/{}/attachments/ ===", targetCandidateId);
        log.info("{\"name\": \"[MIGRATION-TEST] Attachment Mock.pdf\", \"file\": \"" + attachmentUrl + "\", \"description\": \"Attachment de teste\"}");

        String attachmentResponse = targetService.createCandidateAttachment((int) targetCandidateId,
                "[MIGRATION-TEST] Attachment Mock.pdf", attachmentUrl, "Attachment de teste");
        log.info("=== RESPONSE POST attachment (candidate {}) ===", targetCandidateId);
        log.info(attachmentResponse);
    }

    private CandidateTarget.TargetSkill targetSkill(String name, int score) {
        CandidateTarget.TargetSkill skill = new CandidateTarget.TargetSkill();
        skill.setSkillName(name);
        skill.setScore(score);
        return skill;
    }
}
