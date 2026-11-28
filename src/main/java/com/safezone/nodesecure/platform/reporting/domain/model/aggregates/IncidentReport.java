package com.safezone.nodesecure.platform.reporting.domain.model.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Incident report. Manual reports (POST /api/v1/reporting) only fill the original columns.
 * Reports generated from the traceability log (T31) also store warehouse, period and counters;
 * there is at most one generated report per organization, warehouse and period (unique key).
 */
@Entity
@Table(name = "incident_reports", uniqueConstraints = @UniqueConstraint(name = "uk_incident_reports_period",
        columnNames = {"company_id", "warehouse_id", "period_start", "period_end"}))
@Getter
@Setter
@NoArgsConstructor
public class IncidentReport {

    public static final String SOURCE_TRACEABILITY = "TRACEABILITY";
    public static final String MONTHLY_SUMMARY = "MONTHLY_INCIDENT_SUMMARY";
    public static final String ZERO_INCIDENTS = "Cero Incidentes Registrados";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Organization (tenant). Nullable only for rows created before the multi-tenant migration. */
    @Column(name = "company_id")
    private Long companyId;

    @Column(nullable = false)
    private String warehouseName;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String status;

    private String description;

    @Column(name = "warehouse_id")
    private Long warehouseId;

    @Column(name = "period_start")
    private Instant periodStart;

    @Column(name = "period_end")
    private Instant periodEnd;

    private Integer incidentCount;

    private Integer discrepancyCount;

    private Integer disconnectionCount;

    private Integer securityAlertCount;

    @Column(length = 20)
    private String source;

    @Column(length = 120)
    private String generatedBy;

    private Instant generatedAt;

    public IncidentReport(String warehouseName, String eventType, LocalDateTime timestamp, String status, String description) {
        this.warehouseName = warehouseName;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.status = status;
        this.description = description;
    }

    public static IncidentReport generated(Long companyId, Long warehouseId, String warehouseName,
                                           Instant periodStart, Instant periodEnd) {
        IncidentReport report = new IncidentReport(warehouseName, MONTHLY_SUMMARY, null, "GENERATED", null);
        report.companyId = companyId;
        report.warehouseId = warehouseId;
        report.periodStart = periodStart;
        report.periodEnd = periodEnd;
        report.source = SOURCE_TRACEABILITY;
        return report;
    }

    public boolean isGenerated() {
        return SOURCE_TRACEABILITY.equals(source);
    }

    /** A report generated before its period ended is partial and may be refreshed. */
    public boolean isPartial() {
        return generatedAt != null && periodEnd != null && generatedAt.isBefore(periodEnd);
    }

    public void applyCounts(int discrepancies, int disconnections, int securityAlerts, String generatedBy, Instant now) {
        this.discrepancyCount = discrepancies;
        this.disconnectionCount = disconnections;
        this.securityAlertCount = securityAlerts;
        this.incidentCount = discrepancies + disconnections + securityAlerts;
        this.description = incidentCount == 0 ? ZERO_INCIDENTS
                : incidentCount + " incidente(s) registrado(s): " + discrepancies + " discrepancia(s)/merma(s), "
                + disconnections + " desconexión(es) de nodo y " + securityAlerts + " alerta(s) de seguridad.";
        this.generatedBy = generatedBy;
        this.generatedAt = now;
        this.timestamp = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
    }
}
