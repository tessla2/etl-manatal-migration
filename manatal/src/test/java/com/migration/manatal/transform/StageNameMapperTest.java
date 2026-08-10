package com.migration.manatal.transform;

import com.migration.manatal.config.OwnerMappingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StageNameMapperTest {

    private StageNameMapper mapper;

    @BeforeEach
    void setUp() {
        OwnerMappingProperties properties = new OwnerMappingProperties();
        Map<String, String> mapping = new HashMap<>();
        mapping.put("HR Round", "Recruiter Interviews");
        mapping.put("In Client", "Send Candidates to Client");
        properties.setStageMapping(mapping);
        mapper = new StageNameMapper(properties);
    }

    @Test
    void shouldTranslateKnownSourceStage() {
        assertEquals("Recruiter Interviews", mapper.resolve("HR Round"));
        assertEquals("Send Candidates to Client", mapper.resolve("In Client"));
    }

    @Test
    void shouldKeepUnmappedSourceStage() {
        assertEquals("Entrevista", mapper.resolve("Entrevista"));
    }

    @Test
    void shouldMatchKeysWithSpacesStrippedByRelaxedBinding() {
        OwnerMappingProperties properties = new OwnerMappingProperties();
        Map<String, String> mapping = new HashMap<>();
        mapping.put("HRRound", "Recruiter Interviews");
        mapping.put("NewCandidates", "Pre-selection");
        properties.setStageMapping(mapping);
        StageNameMapper springBoundMapper = new StageNameMapper(properties);

        assertEquals("Recruiter Interviews", springBoundMapper.resolve("HR Round"));
        assertEquals("Pre-selection", springBoundMapper.resolve("New Candidates"));
    }

    @Test
    void shouldKeepNullAndBlank() {
        assertNull(mapper.resolve(null));
        assertNull(mapper.resolve(""));
        assertNull(mapper.resolve("   "));
    }
}
