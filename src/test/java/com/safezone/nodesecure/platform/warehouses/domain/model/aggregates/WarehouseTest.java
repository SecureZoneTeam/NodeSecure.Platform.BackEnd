package com.safezone.nodesecure.platform.warehouses.domain.model.aggregates;

import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WarehouseTest {
    private final Instant now = Instant.parse("2026-11-11T12:00:00Z");
    private final StreetAddress address = new StreetAddress("Av. Principal 123", "Ate", "Lima", "PE");

    @Test void createsWarehouseWithActiveStatusAndNoZones() {
        Warehouse warehouse = Warehouse.create(10L, "Ate Warehouse", address, now);
        assertEquals(Long.valueOf(10L), warehouse.getCompanyId());
        assertEquals(WarehouseStatus.ACTIVE, warehouse.getStatus());
        assertEquals(0, warehouse.getZoneCount());
        var event = warehouse.pullDomainEvents().get(0);
        assertInstanceOf(com.safezone.nodesecure.platform.warehouses.domain.model.events.WarehouseRegisteredEvent.class, event);
        var registered = (com.safezone.nodesecure.platform.warehouses.domain.model.events.WarehouseRegisteredEvent) event;
        assertEquals(Long.valueOf(10L), registered.companyId());
        assertEquals("Ate Warehouse", registered.warehouseName());
    }

    @Test void addsAndUpdatesOwnedZones() {
        var shift = OperatingShift.from("08:00", "18:00");
        var existingZone = com.safezone.nodesecure.platform.warehouses.domain.model.entities.WarehouseZone.restore(
                2L, 1L, "Existing", "ATE-01", ZoneRiskLevel.HIGH, shift);
        Warehouse warehouse = Warehouse.restore(1L, 10L, "Ate Warehouse", address, WarehouseStatus.ACTIVE,
                List.of(existingZone), now, now);
        var zone = warehouse.addZone("Storage", "ATE-02", ZoneRiskLevel.HIGH, shift, now);
        assertEquals(2, warehouse.getZoneCount());
        assertEquals(Long.valueOf(1L), zone.getWarehouseId());
        var zoneEvent = warehouse.pullDomainEvents().get(0);
        assertInstanceOf(com.safezone.nodesecure.platform.warehouses.domain.model.events.WarehouseZoneConfiguredEvent.class, zoneEvent);
        assertEquals("ATE-02", ((com.safezone.nodesecure.platform.warehouses.domain.model.events.WarehouseZoneConfiguredEvent) zoneEvent).zoneCode());
        assertTrue(warehouse.hasZoneCode("ate-01", null));
        assertFalse(warehouse.hasZoneCode("ATE-01", existingZone.getId()));
    }

    @Test void requiresCompanyAndWarehouseName() {
        assertThrows(IllegalArgumentException.class, () -> Warehouse.create(null, "Name", address, now));
        assertThrows(IllegalArgumentException.class, () -> Warehouse.create(1L, " ", address, now));
    }
}
