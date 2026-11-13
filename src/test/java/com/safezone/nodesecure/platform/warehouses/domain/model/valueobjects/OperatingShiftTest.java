package com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OperatingShiftTest {
    @Test void acceptsAValidShift() {
        OperatingShift shift = OperatingShift.from("08:00", "18:00");
        assertEquals("08:00", shift.startText());
        assertEquals("18:00", shift.endText());
    }

    @Test void preservesOvernightIntervalsBecauseTheBusinessRuleDoesNotRestrictThem() {
        OperatingShift shift = OperatingShift.from("22:00", "06:00");
        assertEquals("22:00", shift.startText());
        assertEquals("06:00", shift.endText());
    }

    @Test void rejectsTimesOutsideTheHourMinuteFormat() {
        assertThrows(IllegalArgumentException.class, () -> OperatingShift.from("25:00", "06:00"));
        assertThrows(IllegalArgumentException.class, () -> OperatingShift.from("08:00:30", "18:00"));
    }
}
