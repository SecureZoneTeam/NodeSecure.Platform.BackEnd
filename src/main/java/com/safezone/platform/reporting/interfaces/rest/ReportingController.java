package com.safezone.platform.reporting.interfaces.rest;

import com.safezone.platform.reporting.domain.model.aggregates.IncidentReport;
import com.safezone.platform.reporting.domain.repositories.IncidentReportRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reporting")
@Tag(name = "Reporting", description = "Available Audit and Incident Reporting Endpoints")
@CrossOrigin(origins = "*")
public class ReportingController {
    private final IncidentReportRepository incidentReportRepository;

    public ReportingController(IncidentReportRepository incidentReportRepository) {
        this.incidentReportRepository = incidentReportRepository;
    }

    @GetMapping
    public ResponseEntity<List<IncidentReport>> getAllReports() {
        return ResponseEntity.ok(incidentReportRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<IncidentReport> createReport(@RequestBody IncidentReport report) {
        if (report.getTimestamp() == null) {
            report.setTimestamp(LocalDateTime.now());
        }
        IncidentReport saved = incidentReportRepository.save(report);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
