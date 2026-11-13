package com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects;

import java.time.LocalTime;

/** Authorized daily opening interval for a warehouse zone. */
public record OperatingShift(LocalTime start, LocalTime end) {

    public OperatingShift {
        if (start == null || end == null) {
            throw new IllegalArgumentException("warehouse.error.zone.shift.required");
        }
    }

    public static OperatingShift from(String start, String end) {
        if (start == null || end == null
                || !start.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$")
                || !end.matches("^(?:[01]\\d|2[0-3]):[0-5]\\d$")) {
            throw new IllegalArgumentException("warehouse.error.zone.shift.invalid");
        }
        try {
            return new OperatingShift(LocalTime.parse(start), LocalTime.parse(end));
        } catch (RuntimeException exception) {
            if (exception instanceof IllegalArgumentException
                    && exception.getMessage() != null
                    && exception.getMessage().startsWith("warehouse.error.")) {
                throw exception;
            }
            throw new IllegalArgumentException("warehouse.error.zone.shift.invalid");
        }
    }

    public String startText() {
        return start.toString();
    }

    public String endText() {
        return end.toString();
    }
}
