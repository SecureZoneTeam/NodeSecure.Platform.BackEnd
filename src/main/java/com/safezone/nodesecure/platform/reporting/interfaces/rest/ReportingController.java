package com.safezone.nodesecure.platform.reporting.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.reporting.domain.model.aggregates.IncidentReport;
import com.safezone.nodesecure.platform.reporting.domain.repositories.IncidentReportRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** Incident reports of the signed-in user's organization. */
@RestController
@RequestMapping("/api/v1/reporting")
@Tag(name = "Reporting", description = "Incident reports of my organization")
public class ReportingController {

    private final IncidentReportRepository incidentReportRepository;
    private final IamContextFacade iamFacade;

    public ReportingController(IncidentReportRepository incidentReportRepository, IamContextFacade iamFacade) {
        this.incidentReportRepository = incidentReportRepository;
        this.iamFacade = iamFacade;
    }

    @Operation(summary = "Incident reports of my organization")
    @PreAuthorize("hasAuthority('reports.view')")
    @GetMapping
    public ResponseEntity<List<IncidentReportResource>> getAllReports() {
        return ResponseEntity.ok(incidentReportRepository.findAllByCompanyIdOrderByIdAsc(iamFacade.requireCompanyId())
                .stream().map(IncidentReportResource::from).toList());
    }

    @Operation(summary = "Create an incident report")
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

    public record CreateIncidentReportResource(@NotBlank String warehouseName, @NotBlank String eventType,
                                               LocalDateTime timestamp, @NotBlank String status,
                                               String description) {
    }

    public record IncidentReportResource(Long id, Long companyId, String warehouseName, String eventType,
                                         LocalDateTime timestamp, String status, String description) {
        static IncidentReportResource from(IncidentReport report) {
            return new IncidentReportResource(report.getId(), report.getCompanyId(), report.getWarehouseName(),
                    report.getEventType(), report.getTimestamp(), report.getStatus(), report.getDescription());
        }
    }
}
