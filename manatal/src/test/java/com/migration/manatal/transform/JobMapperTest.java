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

class JobMapperTest {

    private JobMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new JobMapper(ownerMapper(Map.of()));
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
        custom.setWorkplace("Remote");
        custom.setTechnicalSkill(List.of("2D - Drawings", "1st Line"));
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
        assertEquals("2026-06-26", target.getOpenAt());
        assertEquals("2026-07-06", target.getCloseAt());
        assertEquals(384181, target.getIndustry());
    }

    @Test
    void shouldMapOwnerViaOwnerMapper() {
        mapper = new JobMapper(ownerMapper(Map.of(810676, 1234)));
        JobSource source = new JobSource();
        source.setOwner(810676);

        JobTarget target = mapper.toTarget(source);

        assertEquals(1234, target.getOwner());
    }

    @Test
    void shouldSetFixedBusinessUnitAndMapCustomFields() {
        JobSource.JobCustomFields custom = new JobSource.JobCustomFields();
        custom.setBusinessUnit("PT -  IT");
        custom.setRate("teste");
        custom.setCategory(List.of("Full Stack Developer"));
        custom.setPortugus(List.of("Obrigatório"));
        custom.setOfficeLocation(List.of("Almada"));
        custom.setGrossMargin(1);
        custom.setConsultantName("Teste");
        custom.setContactName("4796484");

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

    private OwnerMapper ownerMapper(Map<Integer, Integer> mapping) {
        OwnerMappingProperties properties = new OwnerMappingProperties();
        properties.setOwnerMapping(new HashMap<>(mapping));
        return new OwnerMapper(properties);
    }
}
