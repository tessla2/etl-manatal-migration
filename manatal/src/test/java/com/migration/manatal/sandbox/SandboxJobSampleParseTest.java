package com.migration.manatal.sandbox;

import com.migration.manatal.model.job.JobSource;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SandboxJobSampleParseTest {

    @Test
    void sampleParsesToValidJobSource() throws Exception {
        JobSource source = new ObjectMapper().readValue(SandboxJobSample.SAMPLE_SOURCE_JOB, JobSource.class);
        assertNotNull(source);
        assertTrue(source.getDescription().contains("uniquess!"), "description should contain full text");
        assertTrue(source.getDescription().contains("\n"), "description should keep newlines after JSON parse");
    }
}
