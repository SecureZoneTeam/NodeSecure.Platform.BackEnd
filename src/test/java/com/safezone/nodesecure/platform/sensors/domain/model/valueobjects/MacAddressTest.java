package com.safezone.nodesecure.platform.sensors.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MacAddressTest {
    @Test void normalizesToUpperCase() {
        assertEquals("A4:CF:12:9B:1E:44", new MacAddress("a4:cf:12:9b:1e:44").value());
    }

    @Test void rejectsMalformedAddresses() {
        assertThrows(IllegalArgumentException.class, () -> new MacAddress("A4:CF:12:9B:1E"));
        assertThrows(IllegalArgumentException.class, () -> new MacAddress("not-a-mac"));
    }
}
