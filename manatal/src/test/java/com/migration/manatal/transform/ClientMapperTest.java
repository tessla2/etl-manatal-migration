package com.migration.manatal.transform;

import com.migration.manatal.model.client.ClientSource;
import com.migration.manatal.model.client.ClientTarget;
import com.migration.manatal.transform.client.ClientMapper;
import org.junit.jupiter.api.Test;

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
        source.setClientAddress("Rua A, 123");
        source.setClientDescription("Empresa de tecnologia");

        ClientTarget target = mapper.toTarget(source);

        assertNotNull(target);
        assertEquals("Acme Corp", target.getClientName());
        assertEquals("acme.com", target.getClientWebsite());
        assertEquals("Rua A, 123", target.getClientLocation());
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
}
