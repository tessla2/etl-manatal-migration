package com.migration.manatal.transform;

import com.migration.manatal.model.client.ClientSource;
import com.migration.manatal.model.client.ClientTarget;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClientMapperTest {

    private final ClientMapper mapper = new ClientMapper();

    @Test
    void shouldMapSourceToTarget() {
        ClientSource source = new ClientSource();
        source.setClientName("Acme Corp");
        source.setClientWebsite("acme.com");
        source.setClientIndustry(Map.of("id", 1, "name", "Tech"));
        source.setClientAddress("Porto, Portugal");
        source.setClientDescription("Empresa de tecnologia");

        ClientTarget target = mapper.toTarget(source);

        assertNotNull(target);
        assertEquals("Acme Corp", target.getClientName());
        assertEquals("acme.com", target.getClientWebsite());
        assertEquals("Porto, Portugal", target.getClientLocation());
        assertEquals("Empresa de tecnologia", target.getClientDescription());
        assertEquals(1193857, target.getClientOwner());
    }

    @Test
    void shouldReturnNullForNullSource() {
        assertNull(mapper.toTarget(null));
    }

    @Test
    void shouldHandleNullFields() {
        ClientSource source = new ClientSource();

        ClientTarget target = mapper.toTarget(source);

        assertNotNull(target);
        assertNull(target.getClientName());
        assertNull(target.getClientLocation());
    }

    @Test
    void shouldCarryCreatorNameToTargetNote() {
        ClientSource source = new ClientSource();
        ClientSource.SourceNote sourceNote = new ClientSource.SourceNote();
        sourceNote.setContent("Cliente contactado em 30/07");
        sourceNote.setCreatorName("Maria Silva");
        sourceNote.setCreator(5);

        ClientTarget target = mapper.toTarget(source, List.of(), List.of(sourceNote));

        assertEquals(1, target.getNotes().size());
        assertEquals("Maria Silva", target.getNotes().get(0).getCreatorName());
        assertEquals("Cliente contactado em 30/07", target.getNotes().get(0).getContent());
    }
}
