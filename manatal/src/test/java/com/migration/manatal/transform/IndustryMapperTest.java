package com.migration.manatal.transform;

import com.migration.manatal.service.job.ManatalTargetJobService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndustryMapperTest {

    private final ManatalTargetJobService targetService = mock(ManatalTargetJobService.class);

    @Test
    void shouldResolveByName() {
        when(targetService.listIndustries()).thenReturn(List.of(
                new ManatalTargetJobService.IndustryTarget(1, "Accounting / Audit / Tax Services"),
                new ManatalTargetJobService.IndustryTarget(2, "Information Technology")));
        IndustryMapper mapper = new IndustryMapper(targetService);

        assertEquals(1, mapper.resolve("Accounting / Audit / Tax Services"));
        assertEquals(2, mapper.resolve("Information Technology"));
    }

    @Test
    void shouldReturnNullWhenNameNotFound() {
        when(targetService.listIndustries()).thenReturn(List.of(
                new ManatalTargetJobService.IndustryTarget(1, "Information Technology")));
        IndustryMapper mapper = new IndustryMapper(targetService);

        assertNull(mapper.resolve("Unknown Industry"));
    }

    @Test
    void shouldReturnNullForNullOrBlankName() {
        IndustryMapper mapper = new IndustryMapper(targetService);

        assertNull(mapper.resolve(null));
        assertNull(mapper.resolve("  "));
    }

    @Test
    void shouldLoadIndustriesOnlyOnce() {
        when(targetService.listIndustries()).thenReturn(List.of(
                new ManatalTargetJobService.IndustryTarget(1, "Information Technology")));
        IndustryMapper mapper = new IndustryMapper(targetService);

        mapper.resolve("Information Technology");
        mapper.resolve("Information Technology");

        verify(targetService, times(1)).listIndustries();
    }
}
