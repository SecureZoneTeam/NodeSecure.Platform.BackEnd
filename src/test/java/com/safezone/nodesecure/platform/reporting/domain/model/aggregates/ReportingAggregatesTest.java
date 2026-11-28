package com.safezone.nodesecure.platform.reporting.domain.model.aggregates;

import com.safezone.nodesecure.platform.reporting.domain.model.valueobjects.TraceabilityRecordType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportingAggregatesTest {

    private static final Instant NOW = Instant.parse("2026-11-15T12:00:00Z");

    private static TraceabilityRecord.Entry entry(String actor, String description, Instant occurredAt) {
        return new TraceabilityRecord.Entry(1L, 2L, 3L, TraceabilityRecordType.DISCREPANCY, "STOCK_RECONCILIATION",
                "PRODUCT", "9", actor, null, description, occurredAt);
    }

    @Test
    void recordDefaultsToSystemActorAndRecordingTime() {
        TraceabilityRecord record = TraceabilityRecord.of(entry(null, "Adjusted", null), NOW);
        assertThat(record.getActor()).isEqualTo(TraceabilityRecord.SYSTEM_ACTOR);
        assertThat(record.getOccurredAt()).isEqualTo(NOW);
        assertThat(record.getRecordedAt()).isEqualTo(NOW);
        assertThat(record.getRecordType()).isEqualTo("DISCREPANCY");
        assertThat(record.getCompanyId()).isEqualTo(1L);
    }

    @Test
    void recordTruncatesLongDescriptionsAndRequiresAnOrganization() {
        TraceabilityRecord record = TraceabilityRecord.of(entry("fperez", "x".repeat(800), NOW), NOW);
        assertThat(record.getDescription()).hasSize(500);
        assertThatThrownBy(() -> TraceabilityRecord.of(new TraceabilityRecord.Entry(null, null, null,
                TraceabilityRecordType.USER_MANAGEMENT, "USER_REGISTERED", "USER", "x", null, null, "d", NOW), NOW))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void recordTypeParsingRejectsUnknownValues() {
        assertThat(TraceabilityRecordType.parse(" discrepancy ")).isEqualTo(TraceabilityRecordType.DISCREPANCY);
        assertThatThrownBy(() -> TraceabilityRecordType.parse("MERMA")).hasMessage("traceability.error.recordType.invalid");
        assertThat(TraceabilityRecordType.INCIDENTS).containsExactlyInAnyOrder(TraceabilityRecordType.DISCREPANCY,
                TraceabilityRecordType.NODE_DISCONNECTION, TraceabilityRecordType.SECURITY_ALERT);
    }

    @Test
    void monthWithoutIncidentsSaysZeroIncidents() {
        IncidentReport report = IncidentReport.generated(1L, 2L, "Central", Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-11-01T00:00:00Z"));
        report.applyCounts(0, 0, 0, "owner", NOW);
        assertThat(report.getDescription()).isEqualTo(IncidentReport.ZERO_INCIDENTS);
        assertThat(report.getIncidentCount()).isZero();
        assertThat(report.isGenerated()).isTrue();
        assertThat(report.isPartial()).isFalse();
    }

    @Test
    void reportGeneratedBeforeTheEndOfItsMonthIsPartial() {
        IncidentReport report = IncidentReport.generated(1L, 2L, "Central", Instant.parse("2026-11-01T00:00:00Z"),
                Instant.parse("2026-12-01T00:00:00Z"));
        report.applyCounts(2, 1, 3, "owner", NOW);
        assertThat(report.getIncidentCount()).isEqualTo(6);
        assertThat(report.getDescription()).startsWith("6 incidente(s)");
        assertThat(report.isPartial()).isTrue();
    }
}
