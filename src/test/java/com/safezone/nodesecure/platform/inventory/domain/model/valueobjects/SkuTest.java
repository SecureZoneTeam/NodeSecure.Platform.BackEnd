package com.safezone.nodesecure.platform.inventory.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SkuTest {
    @Test
    void normalizesToUppercaseAndTrims() {
        assertEquals("NET-UTP6-305", new Sku("  net-utp6-305 ").value());
    }

    @Test
    void rejectsInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new Sku(null));
        assertThrows(IllegalArgumentException.class, () -> new Sku("ab"));
        assertThrows(IllegalArgumentException.class, () -> new Sku("NET--UTP"));
        assertThrows(IllegalArgumentException.class, () -> new Sku("-NET"));
        assertThrows(IllegalArgumentException.class, () -> new Sku("NET UTP"));
        assertThrows(IllegalArgumentException.class, () -> new Sku("A".repeat(33)));
    }
}
