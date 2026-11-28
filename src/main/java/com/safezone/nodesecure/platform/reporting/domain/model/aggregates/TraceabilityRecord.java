package com.safezone.nodesecure.platform.reporting.domain.model.aggregates;

import com.safezone.nodesecure.platform.reporting.domain.model.valueobjects.TraceabilityRecordType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.Objects;

/**
 * Entry of the tamper-proof traceability log (US08). Records are only inserted: the entity is
 * immutable for Hibernate, every column is non-updatable and the API has no update or delete.
 * Only business identifiers are stored (never passwords, tokens or e-mails).
 */
@Entity
@Immutable
@Table(name = "traceability_records", indexes = {
        @Index(name = "idx_traceability_company_occurred", columnList = "company_id, occurred_at"),
        @Index(name = "idx_traceability_company_warehouse_occurred", columnList = "company_id, warehouse_id, occurred_at"),
        @Index(name = "idx_traceability_company_type_occurred", columnList = "company_id, record_type, occurred_at")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TraceabilityRecord {

    public static final String SYSTEM_ACTOR = "SYSTEM";
    private static final int MAX_DESCRIPTION = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false, updatable = false)
    private Long companyId;

    @Column(name = "warehouse_id", updatable = false)
    private Long warehouseId;

    @Column(updatable = false)
    private Long zoneId;

    @Column(name = "record_type", nullable = false, length = 40, updatable = false)
    private String recordType;

    @Column(nullable = false, length = 60, updatable = false)
    private String eventType;

    @Column(nullable = false, length = 40, updatable = false)
    private String entityType;

    @Column(length = 64, updatable = false)
    private String entityId;

    @Column(nullable = false, length = 120, updatable = false)
    private String actor;

    @Column(updatable = false)
    private Long actorUserId;

    @Column(nullable = false, length = MAX_DESCRIPTION, updatable = false)
    private String description;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false, updatable = false)
    private Instant recordedAt;

    public record Entry(Long companyId, Long warehouseId, Long zoneId, TraceabilityRecordType recordType,
                        String eventType, String entityType, String entityId, String actor, Long actorUserId,
                        String description, Instant occurredAt) {
    }

    public static TraceabilityRecord of(Entry entry, Instant recordedAt) {
        TraceabilityRecord record = new TraceabilityRecord();
        record.companyId = Objects.requireNonNull(entry.companyId(), "companyId");
        record.warehouseId = entry.warehouseId();
        record.zoneId = entry.zoneId();
        record.recordType = Objects.requireNonNull(entry.recordType(), "recordType").name();
        record.eventType = Objects.requireNonNull(entry.eventType(), "eventType");
        record.entityType = Objects.requireNonNull(entry.entityType(), "entityType");
        record.entityId = truncate(entry.entityId(), 64);
        record.actor = entry.actor() == null || entry.actor().isBlank() ? SYSTEM_ACTOR : truncate(entry.actor().trim(), 120);
        record.actorUserId = entry.actorUserId();
        record.description = truncate(Objects.requireNonNull(entry.description(), "description"), MAX_DESCRIPTION);
        record.occurredAt = entry.occurredAt() == null ? recordedAt : entry.occurredAt();
        record.recordedAt = Objects.requireNonNull(recordedAt);
        return record;
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
