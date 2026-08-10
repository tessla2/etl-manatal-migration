package com.migration.manatal.transform;

import com.migration.manatal.config.OwnerMappingProperties;
import com.migration.manatal.model.candidate.CandidateSource;
import com.migration.manatal.model.candidate.CandidateTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CandidateMapperTest {

    private CandidateMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CandidateMapper(ownerMapper(Map.of()));
    }

    @Test
    void shouldMapSourceToTarget() {
        CandidateSource source = new CandidateSource();
        source.setFullName("TESTE 1");
        source.setCandidateLocation("Lisbon, Portugal");
        source.setEmail("teste@gmail.com");
        source.setPhoneNumber("913421111");
        source.setDescription("<p>teste</p>");
        source.setConsent(true);
        source.setOwner(810676);
        CandidateSource.CandidateCustomFields custom = new CandidateSource.CandidateCustomFields();
        custom.setTechnicaldomains("teste");
        custom.setBusinessdomains(List.of("Payments & FinTech"));
        custom.setTechnologies(List.of("Java"));
        custom.setTestlifylink("teste");
        custom.setDocumentaoregularizada(true);
        custom.setPortuguese("C1 (Proficient User)");
        custom.setEnglish(List.of("B1 (Independent User)"));
        custom.setFrench("B2 (Independent User)");
        custom.setSpanish("C2 (Proficient User)");
        custom.setRatehistory("teste");
        custom.setCandidatecertifications("<p>teste</p>");
        custom.setName("teste");
        custom.setDate("2026-08-06T23:00:00Z");
        custom.setMaintechnologies(List.of("C++", "PHP"));
        source.setCustomFields(custom);

        CandidateTarget target = mapper.toTarget(source);

        assertNotNull(target);
        assertEquals("TESTE 1", target.getFullName());
        assertEquals("Lisbon, Portugal", target.getCandidateLocation());
        assertEquals("teste@gmail.com", target.getEmail());
        assertEquals("913421111", target.getPhoneNumber());
        assertEquals("teste", target.getDescription());
        assertEquals(true, target.getConsent());
        assertEquals(1193857, target.getOwner());
    }

    @Test
    void shouldMapOwnerViaOwnerMapper() {
        mapper = new CandidateMapper(ownerMapper(Map.of(810676, 1234)));
        CandidateSource source = new CandidateSource();
        source.setOwner(810676);

        CandidateTarget target = mapper.toTarget(source);

        assertEquals(1234, target.getOwner());
    }

    @Test
    void shouldMapCustomFields() {
        CandidateSource.CandidateCustomFields custom = new CandidateSource.CandidateCustomFields();
        custom.setTechnicaldomains("teste");
        custom.setBusinessdomains(List.of("Payments & FinTech"));
        custom.setTechnologies(List.of("Java"));
        custom.setTestlifylink("teste");
        custom.setDocumentaoregularizada(true);
        custom.setPortuguese("C1 (Proficient User)");
        custom.setEnglish(List.of("B1 (Independent User)"));
        custom.setFrench("B2 (Independent User)");
        custom.setSpanish("C2 (Proficient User)");
        custom.setRatehistory("<p>teste</p>");
        custom.setCandidatecertifications("<p>teste</p>");
        custom.setName("teste");
        custom.setDate("2026-08-06T23:00:00Z");
        custom.setMaintechnologies(List.of("C++", "PHP"));

        CandidateSource source = new CandidateSource();
        source.setCustomFields(custom);

        CandidateTarget target = mapper.toTarget(source);

        CandidateTarget.CandidateCustomFields mapped = target.getCustomFields();
        assertNotNull(mapped);
        assertEquals("teste", mapped.getTechnicaldomains());
        assertEquals(List.of("Payments & FinTech"), mapped.getBusinessdomains());
        assertEquals(List.of("Java"), mapped.getCategory());
        assertEquals("teste", mapped.getTestlifylink());
        assertEquals(true, mapped.getWorkVisaEuCitizenship());
        assertEquals("C1 (Proficient User)", mapped.getPortugueselevel());
        assertEquals("B1 (Independent User)", mapped.getEnglishlevel());
        assertEquals("B2 (Independent User)", mapped.getFrenchlevel());
        assertEquals("C2 (Proficient User)", mapped.getSpanishlevel());
        assertEquals("<p>teste</p>", mapped.getRatehistory());
        assertEquals("<p>teste</p>", mapped.getCandidatecertifications());
        assertEquals("teste", mapped.getReferenceName());
        assertEquals("2026-08-06T23:00:00Z", mapped.getDate());
        assertEquals(List.of("C++", "PHP"), mapped.getTechnologies());
    }

    @Test
    void shouldUseNewLevelsOverOldLevels() {
        CandidateSource.CandidateCustomFields custom = new CandidateSource.CandidateCustomFields();
        custom.setEnglish(List.of("B1 (Independent User)"));
        custom.setFrench("B2 (Independent User)");
        custom.setSpanish("C1 (Proficient User)");
        custom.setPortuguese("C2 (Proficient User)");
        custom.setEnglishlevel("A1 (Basic User)");
        custom.setFrenchlevel("A1 (Basic User)");
        custom.setSpanishlevel("A1 (Basic User)");
        custom.setPortugueselevel("A1 (Basic User)");

        CandidateSource source = new CandidateSource();
        source.setCustomFields(custom);

        CandidateTarget.CandidateCustomFields mapped = mapper.toTarget(source).getCustomFields();

        assertEquals("B1 (Independent User)", mapped.getEnglishlevel());
        assertEquals("B2 (Independent User)", mapped.getFrenchlevel());
        assertEquals("C1 (Proficient User)", mapped.getSpanishlevel());
        assertEquals("C2 (Proficient User)", mapped.getPortugueselevel());
    }

    @Test
    void shouldFallbackToOldLevelsWhenNewAbsent() {
        CandidateSource.CandidateCustomFields custom = new CandidateSource.CandidateCustomFields();
        custom.setEnglishlevel("B1 (Independent User)");
        custom.setFrenchlevel("A1 (Basic User)");

        CandidateSource source = new CandidateSource();
        source.setCustomFields(custom);

        CandidateTarget.CandidateCustomFields mapped = mapper.toTarget(source).getCustomFields();

        assertEquals("B1 (Independent User)", mapped.getEnglishlevel());
        assertEquals("A1 (Basic User)", mapped.getFrenchlevel());
        assertNull(mapped.getSpanishlevel());
        assertNull(mapped.getPortugueselevel());
    }

    @Test
    void shouldMapSummaryToDescription() {
        CandidateSource source = new CandidateSource();
        source.setDescription("<p>descricao nativa</p>");
        CandidateSource.CandidateCustomFields custom = new CandidateSource.CandidateCustomFields();
        custom.setSummary("<p>resumo profissional</p>");
        source.setCustomFields(custom);

        CandidateTarget target = mapper.toTarget(source);

        assertEquals("resumo profissional", target.getDescription());
    }

    @Test
    void shouldKeepNativeDescriptionWhenSummaryAbsent() {
        CandidateSource source = new CandidateSource();
        source.setDescription("<p>descricao nativa</p>");

        CandidateTarget target = mapper.toTarget(source);

        assertEquals("descricao nativa", target.getDescription());
    }

    @Test
    void shouldMapSkillsNotesAndNationalities() {
        CandidateSource.Skill skill = new CandidateSource.Skill();
        skill.setSkillName("JavaFX");
        skill.setScore(1);

        CandidateSource.CandidateNote note = new CandidateSource.CandidateNote();
        note.setInfo("<p>teste</p>");
        note.setCreator(1193857L);
        note.setCreatedAt("2026-08-05T11:28:43.621827Z");

        CandidateSource.Nationality nationality = new CandidateSource.Nationality();
        nationality.setCountry("Burundi");

        CandidateSource source = new CandidateSource();
        source.setFullName("TESTE 1");

        CandidateTarget target = mapper.toTarget(source, List.of(note), List.of(nationality), List.of(skill));

        assertEquals(1, target.getSkills().size());
        assertEquals("JavaFX", target.getSkills().get(0).getSkillName());
        assertEquals(1, target.getSkills().get(0).getScore());

        assertEquals(1, target.getNotes().size());
        assertEquals("<p>teste</p>", target.getNotes().get(0).getContent());
        assertEquals("2026-08-05T11:28:43.621827Z", target.getNotes().get(0).getCreatedAt());

        assertEquals(1, target.getNationalities().size());
        assertEquals("Burundi", target.getNationalities().get(0).getCountry());
    }

    @Test
    void shouldReturnNullForNullSource() {
        assertNull(mapper.toTarget(null));
    }

    @Test
    void shouldHandleNullFields() {
        CandidateTarget target = mapper.toTarget(new CandidateSource());

        assertNotNull(target);
        assertNull(target.getFullName());
        assertEquals(1193857, target.getOwner());
        assertNotNull(target.getCustomFields());
        assertTrue(target.getSkills().isEmpty());
        assertTrue(target.getNotes().isEmpty());
        assertTrue(target.getNationalities().isEmpty());
    }

    private OwnerMapper ownerMapper(Map<Integer, Integer> mapping) {
        OwnerMappingProperties properties = new OwnerMappingProperties();
        properties.setOwnerMapping(new HashMap<>(mapping));
        return new OwnerMapper(properties);
    }
}
