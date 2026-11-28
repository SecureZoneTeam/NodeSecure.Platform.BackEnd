package com.safezone.nodesecure.platform.reporting.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.reporting.application.internal.services.IncidentReportService;
import com.safezone.nodesecure.platform.reporting.application.internal.services.IncidentReportService.Generation;
import com.safezone.nodesecure.platform.reporting.domain.model.aggregates.IncidentReport;
import com.safezone.nodesecure.platform.reporting.domain.model.aggregates.TraceabilityRecord;
import com.safezone.nodesecure.platform.reporting.domain.repositories.IncidentReportRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Incident reports of the signed-in user's organization. Manual reports keep working as before;
 * T31 adds reports generated from the traceability log, their lookup and a CSV (Excel) export.
 */
@RestController
@RequestMapping("/api/v1/reporting")
@Tag(name = "Reporting", description = "Incident reports of my organization")
public class ReportingController {

    private final IncidentReportRepository incidentReportRepository;
    private final IncidentReportService reportService;
    private final IamContextFacade iamFacade;

    public ReportingController(IncidentReportRepository incidentReportRepository, IncidentReportService reportService,
                               IamContextFacade iamFacade) {
        this.incidentReportRepository = incidentReportRepository;
        this.reportService = reportService;
        this.iamFacade = iamFacade;
    }

    @Operation(summary = "Incident reports of my organization (manual and generated)")
    @PreAuthorize("hasAuthority('reports.view')")
    @GetMapping
    public ResponseEntity<List<IncidentReportResource>> getAllReports() {
        return ResponseEntity.ok(reportService.findAll().stream().map(IncidentReportResource::from).toList());
    }

    @Operation(summary = "Get an incident report of my organization")
    @ApiResponse(responseCode = "404", description = "Not found or not visible for the user")
    @PreAuthorize("hasAuthority('reports.view')")
    @GetMapping("/{id}")
    public ResponseEntity<IncidentReportResource> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(IncidentReportResource.from(reportService.getById(id)));
    }

    @Operation(summary = "Create an incident report manually")
    @PreAuthorize("hasAuthority('reports.create')")
    @PostMapping
    public ResponseEntity<IncidentReportResource> createReport(@Valid @RequestBody CreateIncidentReportResource resource) {
        IncidentReport report = new IncidentReport(resource.warehouseName().trim(), resource.eventType().trim(),
                resource.timestamp() == null ? LocalDateTime.now() : resource.timestamp(),
                resource.status().trim(), resource.description());
        report.setCompanyId(iamFacade.requireCompanyId());
        IncidentReport saved = incidentReportRepository.save(report);
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentReportResource.from(saved));
    }

    @Operation(summary = "Generate the monthly incident report of a warehouse from the traceability log",
            description = "Counts discrepancies/shrinkage, node disconnections and security alerts of the month. "
                    + "A month without incidents is reported as 'Cero Incidentes Registrados'. Generating the same "
                    + "warehouse and month again returns the existing report (refreshed if the month was still open).")
    @ApiResponse(responseCode = "201", description = "Report generated")
    @ApiResponse(responseCode = "200", description = "The report already existed (no duplicate is created)")
    @ApiResponse(responseCode = "400", description = "Invalid or future period")
    @ApiResponse(responseCode = "403", description = "Warehouse outside of the user's scope")
    @ApiResponse(responseCode = "404", description = "Warehouse not found in my organization")
    @PreAuthorize("hasAuthority('reports.create')")
    @PostMapping("/incident-reports")
    public ResponseEntity<IncidentReportResource> generate(@Valid @RequestBody GenerateIncidentReportResource resource) {
        Generation generation = reportService.generateMonthly(resource.warehouseId(),
                YearMonth.of(resource.year(), resource.month()));
        return ResponseEntity.status(generation.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(IncidentReportResource.from(generation.report()));
    }

    @Operation(summary = "Export an incident report as CSV (opens in Excel)")
    @ApiResponse(responseCode = "200", description = "CSV file with the summary and the incidents of the period")
    @PreAuthorize("hasAuthority('reports.view')")
    @GetMapping(value = "/{id}/export", produces = "text/csv")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        IncidentReport report = reportService.getById(id);
        String csv = IncidentReportCsv.render(report, reportService.incidentsOf(report));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"incident-report-" + id + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    public record CreateIncidentReportResource(@NotBlank String warehouseName, @NotBlank String eventType,
                                               LocalDateTime timestamp, @NotBlank String status,
                                               String description) {
    }

    @Schema(description = "Warehouse and month of the report")
    public record GenerateIncidentReportResource(@Schema(example = "1") @NotNull @Positive Long warehouseId,
                                                 @Schema(example = "2026") @NotNull @Min(2000) @Max(2100) Integer year,
                                                 @Schema(example = "11") @NotNull @Min(1) @Max(12) Integer month) {
    }

    public record IncidentReportResource(Long id, Long companyId, String warehouseName, String eventType,
                                         LocalDateTime timestamp, String status, String description,
                                         Long warehouseId, Instant periodStart, Instant periodEnd,
                                         Integer incidentCount, Integer discrepancyCount, Integer disconnectionCount,
                                         Integer securityAlertCount, String source, String generatedBy) {
        static IncidentReportResource from(IncidentReport report) {
            return new IncidentReportResource(report.getId(), report.getCompanyId(), report.getWarehouseName(),
                    report.getEventType(), report.getTimestamp(), report.getStatus(), report.getDescription(),
                    report.getWarehouseId(), report.getPeriodStart(), report.getPeriodEnd(), report.getIncidentCount(),
                    report.getDiscrepancyCount(), report.getDisconnectionCount(), report.getSecurityAlertCount(),
                    report.getSource(), report.getGeneratedBy());
        }
    }

    /** CSV rendering of a report; every value is quoted so commas or quotes in descriptions are safe. */
    static final class IncidentReportCsv {

        private IncidentReportCsv() {
        }

        static String render(IncidentReport report, List<TraceabilityRecord> incidents) {
            StringBuilder csv = new StringBuilder("﻿");
            row(csv, "Reporte de incidentes", String.valueOf(report.getId()));
            row(csv, "Almacén", report.getWarehouseName());
            row(csv, "Periodo", report.getPeriodStart() + " - " + report.getPeriodEnd());
            row(csv, "Resumen", report.getDescription());
            row(csv, "Discrepancias/mermas", String.valueOf(report.getDiscrepancyCount()));
            row(csv, "Desconexiones de nodo", String.valueOf(report.getDisconnectionCount()));
            row(csv, "Alertas de seguridad", String.valueOf(report.getSecurityAlertCount()));
            row(csv, "Generado", report.getGeneratedAt() + " por " + report.getGeneratedBy());
            csv.append("\r\n");
            row(csv, "Fecha (UTC)", "Tipo", "Evento", "Responsable", "Descripción");
            if (incidents.isEmpty()) {
                row(csv, "", "", "", "", IncidentReport.ZERO_INCIDENTS);
            }
            incidents.forEach(record -> row(csv, String.valueOf(record.getOccurredAt()), record.getRecordType(),
                    record.getEventType(), record.getActor(), record.getDescription()));
            return csv.toString();
        }

        private static void row(StringBuilder csv, String... values) {
            for (int i = 0; i < values.length; i++) {
                if (i > 0) csv.append(',');
                String value = values[i] == null ? "" : values[i];
                // Neutralizes spreadsheet formulas (CSV injection) and escapes quotes.
                if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value;
                csv.append('"').append(value.replace("\"", "\"\"")).append('"');
            }
            csv.append("\r\n");
        }
    }
}
