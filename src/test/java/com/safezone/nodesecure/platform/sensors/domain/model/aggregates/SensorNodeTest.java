package com.safezone.nodesecure.platform.sensors.domain.model.aggregates;

import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.*;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SensorNodeTest {
    @Test void newNodeStartsOfflineAndRegistersEvent() {
        Instant now = Instant.parse("2026-11-11T12:00:00Z");
        SensorNode node = SensorNode.create(2L, 5L, new MacAddress("A4:CF:12:9B:1E:44"),
                "ESP32-WROOM-32", SensorType.MAGNETIC_CONTACT, now);
        assertEquals(ConnectionStatus.OFFLINE, node.getConnectionStatus());
        assertNull(node.getLastPingAt());
        var event = node.pullDomainEvents().get(0);
        assertInstanceOf(com.safezone.nodesecure.platform.sensors.domain.model.events.SensorNodeRegisteredEvent.class, event);
        var registered = (com.safezone.nodesecure.platform.sensors.domain.model.events.SensorNodeRegisteredEvent) event;
        assertEquals(Long.valueOf(2L), registered.companyId());
        assertEquals(Long.valueOf(5L), registered.zoneId());
        assertEquals("A4:CF:12:9B:1E:44", registered.macAddress());
    }

    @Test void pingMarksNodeOnlineAndTimeoutDetectsDisconnection() {
        Instant now = Instant.parse("2026-11-11T12:00:00Z");
        SensorNode node = SensorNode.create(2L, 5L, new MacAddress("A4:CF:12:9B:1E:44"),
                "ESP32-WROOM-32", SensorType.MAGNETIC_CONTACT, now);
        node.registerPing(now);
        assertEquals(ConnectionStatus.ONLINE, node.getConnectionStatus());
        assertFalse(node.connectionExpired(now.plusSeconds(30), Duration.ofSeconds(120)));
        assertTrue(node.connectionExpired(now.plusSeconds(121), Duration.ofSeconds(120)));
        node.markOffline(now.plusSeconds(121));
        assertEquals(ConnectionStatus.OFFLINE, node.getConnectionStatus());
    }
}
