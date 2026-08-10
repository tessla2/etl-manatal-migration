package com.migration.manatal.transform;

import com.migration.manatal.config.OwnerMappingProperties;
import com.migration.manatal.model.job.JobSource;
import com.migration.manatal.model.job.JobTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobMapperTest {

    private JobMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new JobMapper(ownerMapper(Map.of()), industryMapper());
    }

    @Test
    void shouldMapSourceToTarget() {
        JobSource source = new JobSource();
        source.setPositionName("Teste 2");
        source.setHeadcount(1);
        source.setStatus("active");
        source.setContractDetails("full_time");
        source.setCity("Almada");
        source.setCountry("Portugal");
        source.setOpenAt("2026-06-26T19:01:02.533796Z");
        source.setCloseAt("2026-07-06T00:00:00Z");

        JobSource.Industry industry = new JobSource.Industry();
        industry.setId(384181);
        industry.setName("Accounting / Audit / Tax Services");
        source.setIndustry(industry);

        JobSource.JobCustomFields custom = new JobSource.JobCustomFields();
        custom.setExperienceLevelOfficial(List.of("Middle"));
        custom.setEnglishLevel("Beginner (A1,A2)");
        custom.setJobModel("Remote");
        custom.setTechnicalSkill(List.of("2D - Drawings", "1st Line"));
        custom.setTechnologies(List.of("Python"));
        custom.setStartDateJob("2026-08-03T00:00");
        custom.setStartDateSyffer("2026-08-04T00:00");
        custom.setStartDateOportunity("2026-08-11T00:00");
        source.setCustomFields(custom);

        JobTarget target = mapper.toTarget(source);

        assertNotNull(target);
        assertEquals("Teste 2", target.getPositionName());
        assertEquals(1, target.getHeadcount());
        assertEquals(1193857, target.getOwner());
        assertEquals("active", target.getStatus());
        assertEquals("full_time", target.getContractDetails());
        assertEquals("Almada", target.getCity());
        assertEquals("Portugal", target.getCountry());
        assertEquals("EUR", target.getCurrency());
        assertEquals("2026-06-26", target.getOpenAt());
        assertEquals("2026-07-06", target.getCloseAt());
        assertEquals(555, target.getIndustry());
        assertEquals(List.of("Middle"), target.getCustomFields().getExperienceLevel());
        assertEquals(List.of("2D - Drawings", "1st Line"), target.getCustomFields().getMandatorySkills());
        assertEquals("Python", target.getCustomFields().getSkillNotes());
        assertEquals("2026-08-11T00:00", target.getCustomFields().getStartDateJob());
        assertEquals("2026-08-04T00:00", target.getCustomFields().getStartDateSyffer());
    }

    @Test
    void shouldJoinTechnologiesIntoSkillNotes() {
        JobSource.JobCustomFields custom = new JobSource.JobCustomFields();
        custom.setTechnologies(List.of("Python", "Java"));

        JobSource source = new JobSource();
        source.setCustomFields(custom);

        JobTarget target = mapper.toTarget(source);

        assertNull(target.getCustomFields().getMandatorySkills());
        assertEquals("Python, Java", target.getCustomFields().getSkillNotes());
    }

    @Test
    void shouldMapOwnerViaOwnerMapper() {
        mapper = new JobMapper(ownerMapper(Map.of(810676, 1234)), industryMapper());
        JobSource source = new JobSource();
        source.setOwner(810676);

        JobTarget target = mapper.toTarget(source);

        assertEquals(1234, target.getOwner());
    }

    @Test
    void shouldSetFixedBusinessUnitAndMapCustomFields() {
        JobSource.JobCustomFields custom = new JobSource.JobCustomFields();
        custom.setBusinessUnit("PT -  IT");
        custom.setClientRate("<p>teste</p>");
        custom.setCategory(List.of("Full Stack Developer"));
        custom.setPortugus(List.of("Obrigatório"));
        custom.setOfficeLocation(List.of("Almada"));
        custom.setGrossMargin(1);
        custom.setConsultantName("Teste");
        custom.setContactName("4796484");
        custom.setJobModelDetails("Detalhe adicional");
        custom.setProjectNotes("<p>Notas do projeto</p>");
        custom.setTechnologies(List.of("Python", "SQL"));
        custom.setLostReason("Closed with another consultancy");

        JobSource source = new JobSource();
        source.setCustomFields(custom);

        JobTarget target = mapper.toTarget(source);

        JobTarget.JobCustomFields mapped = target.getCustomFields();
        assertNotNull(mapped);
        assertEquals("PT - IT", mapped.getBusinessUnit());
        assertEquals("teste", mapped.getRate());
        assertEquals(List.of("Full Stack Developer"), mapped.getCategory());
        assertEquals(List.of("Obrigatório"), mapped.getPortugus());
        assertEquals(List.of("Almada"), mapped.getOfficeLocation());
        assertEquals(1, mapped.getGrossMargin());
        assertEquals("Teste", mapped.getConsultantName());
        assertEquals("4796484", mapped.getContactName());
        assertEquals("Detalhe adicional", mapped.getJobAdditionalInformation());
        assertEquals("<p>Notas do projeto</p>", mapped.getProjectNotes());
        assertEquals("Python, SQL", mapped.getSkillNotes());
        assertEquals("Closed with another consultancy", mapped.getLostReason());
    }

    @Test
    void shouldReturnNullForNullSource() {
        assertNull(mapper.toTarget(null));
    }

    @Test
    void shouldHandleNullFields() {
        JobTarget target = mapper.toTarget(new JobSource());

        assertNotNull(target);
        assertNull(target.getPositionName());
        assertEquals("PT - IT", target.getCustomFields().getBusinessUnit());
    }

    @Test
    void shouldLeaveIndustryEmptyWhenNotFoundInTarget() {
        IndustryMapper industryMapper = mock(IndustryMapper.class);
        when(industryMapper.resolve("Accounting / Audit / Tax Services")).thenReturn(null);
        mapper = new JobMapper(ownerMapper(Map.of()), industryMapper);

        JobSource source = new JobSource();
        JobSource.Industry industry = new JobSource.Industry();
        industry.setId(384181);
        industry.setName("Accounting / Audit / Tax Services");
        source.setIndustry(industry);

        JobTarget target = mapper.toTarget(source);

        assertNull(target.getIndustry());
    }

    private OwnerMapper ownerMapper(Map<Integer, Integer> mapping) {
        OwnerMappingProperties properties = new OwnerMappingProperties();
        properties.setOwnerMapping(new HashMap<>(mapping));
        return new OwnerMapper(properties);
    }

    private IndustryMapper industryMapper() {
        IndustryMapper industryMapper = mock(IndustryMapper.class);
        when(industryMapper.resolve("Accounting / Audit / Tax Services")).thenReturn(555);
        return industryMapper;
    }
}
