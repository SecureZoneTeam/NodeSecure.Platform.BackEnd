package com.safezone.nodesecure.platform.inventory.domain.model.aggregates;

import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class StockMovementTest {
    private static final Instant NOW = Instant.parse("2026-10-02T08:15:00Z");

    @Test
    void entryIgnoresJustificationAndEmitsEvent() {
        var movement = StockMovement.register(1L, 1L, StockMovementType.ENTRY, 50, 0, 50, "fperez", "ignored text here", NOW);
        assertNull(movement.getJustification());
        assertEquals(1, movement.pullDomainEvents().size());
        assertTrue(movement.pullDomainEvents().isEmpty());
    }

    @Test
    void entryAndExitRequirePositiveQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> StockMovement.register(1L, 1L, StockMovementType.ENTRY, 0, 0, 0, "fperez", null, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> StockMovement.register(1L, 1L, StockMovementType.EXIT, -2, 5, 5, "fperez", null, NOW));
    }

    @Test
    void reconciliationRequiresJustificationBetween10And300Characters() {
        assertThrows(IllegalArgumentException.class,
                () -> StockMovement.register(1L, 1L, StockMovementType.RECONCILIATION, 7, 10, 7, "lravello", null, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> StockMovement.register(1L, 1L, StockMovementType.RECONCILIATION, 7, 10, 7, "lravello", "too short", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> StockMovement.register(1L, 1L, StockMovementType.RECONCILIATION, 7, 10, 7, "lravello", "x".repeat(301), NOW));
        var ok = StockMovement.register(1L, 1L, StockMovementType.RECONCILIATION, 0, 10, 0, "lravello",
                "  Physical count after alert  ", NOW);
        assertEquals("Physical count after alert", ok.getJustification());
    }

    @Test
    void requiresProductWarehouseAndPerformer() {
        assertThrows(IllegalArgumentException.class,
                () -> StockMovement.register(0L, 1L, StockMovementType.ENTRY, 1, 0, 1, "fperez", null, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> StockMovement.register(1L, null, StockMovementType.ENTRY, 1, 0, 1, "fperez", null, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> StockMovement.register(1L, 1L, StockMovementType.ENTRY, 1, 0, 1, " ", null, NOW));
    }

    @Test
    void typeParsingIsCaseInsensitive() {
        assertEquals(StockMovementType.EXIT, StockMovementType.fromValue(" exit "));
        assertThrows(IllegalArgumentException.class, () -> StockMovementType.fromValue("MOVE"));
        assertThrows(IllegalArgumentException.class, () -> StockMovementType.fromValue(null));
    }
}
