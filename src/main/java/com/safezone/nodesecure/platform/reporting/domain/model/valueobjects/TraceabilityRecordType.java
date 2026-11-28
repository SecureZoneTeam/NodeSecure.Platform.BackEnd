package com.safezone.nodesecure.platform.reporting.domain.model.valueobjects;

import java.util.EnumSet;
import java.util.Set;

/** Category of a traceability record, used by the filters of the log (US10) and by incident reports (US11). */
public enum TraceabilityRecordType {
    PHYSICAL_ACCESS,
    NODE_DISCONNECTION,
    INVENTORY_MOVEMENT,
    DISCREPANCY,
    SECURITY_ALERT,
    PRODUCT_CATALOG,
    USER_MANAGEMENT,
    SENSOR_MANAGEMENT;

    /** Types counted as incidents in the monthly report: shrinkage/discrepancies, disconnections and alerts. */
    public static final Set<TraceabilityRecordType> INCIDENTS = EnumSet.of(DISCREPANCY, NODE_DISCONNECTION, SECURITY_ALERT);

    public static TraceabilityRecordType parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("traceability.error.recordType.invalid");
        }
    }
}
