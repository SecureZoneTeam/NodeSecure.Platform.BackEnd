package com.safezone.nodesecure.platform.reporting.domain.repositories;

import com.safezone.nodesecure.platform.reporting.domain.model.aggregates.TraceabilityRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Append-only log: the repository deliberately exposes no update or delete operation. */
@org.springframework.stereotype.Repository
public interface TraceabilityRecordRepository extends Repository<TraceabilityRecord, Long> {

    TraceabilityRecord save(TraceabilityRecord record);

    Optional<TraceabilityRecord> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Combinable filters. Collections are always non-empty: the boolean flags say whether they apply
     * ({@code restrictToScope} hides records outside the worker's warehouses, including org-level ones).
     */
    @Query("""
            SELECT r FROM TraceabilityRecord r
            WHERE r.companyId = :companyId
              AND (:warehouseId IS NULL OR r.warehouseId = :warehouseId)
              AND (:restrictToScope = false OR r.warehouseId IN :scope)
              AND (:filterTypes = false OR r.recordType IN :recordTypes)
              AND (:eventType IS NULL OR r.eventType = :eventType)
              AND (:from IS NULL OR r.occurredAt >= :from)
              AND (:to IS NULL OR r.occurredAt < :to)
            """)
    Page<TraceabilityRecord> search(@Param("companyId") Long companyId,
                                    @Param("warehouseId") Long warehouseId,
                                    @Param("restrictToScope") boolean restrictToScope,
                                    @Param("scope") Collection<Long> scope,
                                    @Param("filterTypes") boolean filterTypes,
                                    @Param("recordTypes") Collection<String> recordTypes,
                                    @Param("eventType") String eventType,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to,
                                    Pageable pageable);

    List<TraceabilityRecord> findAllByCompanyIdAndWarehouseIdAndRecordTypeInAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscIdAsc(
            Long companyId, Long warehouseId, Collection<String> recordTypes, Instant from, Instant to);
}
