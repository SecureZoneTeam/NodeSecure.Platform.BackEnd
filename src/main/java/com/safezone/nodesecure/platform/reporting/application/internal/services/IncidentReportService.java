package com.safezone.nodesecure.platform.reporting.application.internal.services;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.reporting.domain.model.aggregates.IncidentReport;
import com.safezone.nodesecure.platform.reporting.domain.model.aggregates.TraceabilityRecord;
import com.safezone.nodesecure.platform.reporting.domain.model.valueobjects.TraceabilityRecordType;
import com.safezone.nodesecure.platform.reporting.domain.repositories.IncidentReportRepository;
import com.safezone.nodesecure.platform.reporting.domain.repositories.TraceabilityRecordRepository;
import com.safezone.nodesecure.platform.shared.domain.exceptions.ResourceNotFoundException;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/**
 * T31: monthly incident report of a warehouse generated from the traceability log (US11).
 * Incidents are the records of type DISCREPANCY, NODE_DISCONNECTION and SECURITY_ALERT.
 * Generating the same warehouse and month again never creates a second report: a closed period
 * returns the stored report; a period that was still open when generated is refreshed in place.
 */
@Service
public class IncidentReportService {

    private static final List<String> INCIDENT_TYPES = TraceabilityRecordType.INCIDENTS.stream().map(Enum::name).toList();

    private final IncidentReportRepository reports;
    private final TraceabilityRecordRepository records;
    private final TraceabilityQueryService traceability;
    private final WarehouseContextFacade warehouseFacade;
    private final IamContextFacade iamFacade;
    private final Clock clock;

    public IncidentReportService(IncidentReportRepository reports, TraceabilityRecordRepository records,
                                 TraceabilityQueryService traceability, WarehouseContextFacade warehouseFacade,
                                 IamContextFacade iamFacade, Clock clock) {
        this.reports = reports;
        this.records = records;
        this.traceability = traceability;
        this.warehouseFacade = warehouseFacade;
        this.iamFacade = iamFacade;
        this.clock = clock;
    }

    public record Generation(IncidentReport report, boolean created) {
    }

    @Transactional
    public Generation generateMonthly(Long warehouseId, YearMonth month) {
        Long companyId = iamFacade.requireCompanyId();
        traceability.requireAccessibleWarehouse(warehouseId, companyId);
        Instant now = clock.instant();
        Instant start = month.atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = month.plusMonths(1).atDay(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        if (start.isAfter(now)) {
            throw new IllegalArgumentException("reports.error.period.future");
        }
        var existing = reports.findByCompanyIdAndWarehouseIdAndPeriodStartAndPeriodEnd(companyId, warehouseId, start, end);
        if (existing.isPresent() && !existing.get().isPartial()) {
            return new Generation(existing.get(), false);
        }
        IncidentReport report = existing.orElseGet(() -> IncidentReport.generated(companyId, warehouseId,
                warehouseFacade.findWarehouseName(warehouseId, companyId).orElse("Warehouse " + warehouseId), start, end));
        List<TraceabilityRecord> incidents = incidents(companyId, warehouseId, start, end);
        report.applyCounts(count(incidents, TraceabilityRecordType.DISCREPANCY),
                count(incidents, TraceabilityRecordType.NODE_DISCONNECTION),
                count(incidents, TraceabilityRecordType.SECURITY_ALERT),
                iamFacade.currentUsername(), now);
        return new Generation(reports.save(report), existing.isEmpty());
    }

    @Transactional(readOnly = true)
    public List<IncidentReport> findAll() {
        Long companyId = iamFacade.requireCompanyId();
        Set<Long> scope = iamFacade.warehouseScope();
        return reports.findAllByCompanyIdOrderByIdAsc(companyId).stream()
                .filter(report -> visible(report, scope))
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentReport getById(Long id) {
        Set<Long> scope = iamFacade.warehouseScope();
        return reports.findByIdAndCompanyId(id, iamFacade.requireCompanyId())
                .filter(report -> visible(report, scope))
                .orElseThrow(() -> new ResourceNotFoundException("reports.error.notFound", id));
    }

    /** Incident records behind a generated report (empty for manual reports). */
    @Transactional(readOnly = true)
    public List<TraceabilityRecord> incidentsOf(IncidentReport report) {
        if (!report.isGenerated() || report.getWarehouseId() == null) {
            return List.of();
        }
        return incidents(report.getCompanyId(), report.getWarehouseId(), report.getPeriodStart(), report.getPeriodEnd());
    }

    private List<TraceabilityRecord> incidents(Long companyId, Long warehouseId, Instant start, Instant end) {
        return records.findAllByCompanyIdAndWarehouseIdAndRecordTypeInAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAscIdAsc(
                companyId, warehouseId, INCIDENT_TYPES, start, end);
    }

    private static int count(List<TraceabilityRecord> incidents, TraceabilityRecordType type) {
        return (int) incidents.stream().filter(record -> type.name().equals(record.getRecordType())).count();
    }

    /** Workers do not see generated reports of warehouses outside their scope. */
    private static boolean visible(IncidentReport report, Set<Long> scope) {
        return scope == null || report.getWarehouseId() == null || scope.contains(report.getWarehouseId());
    }
}
