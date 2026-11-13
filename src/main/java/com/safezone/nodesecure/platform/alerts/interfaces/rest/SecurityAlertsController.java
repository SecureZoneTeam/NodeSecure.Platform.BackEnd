package com.safezone.nodesecure.platform.alerts.interfaces.rest;

import com.safezone.nodesecure.platform.alerts.application.commandservices.SecurityAlertCommandService;
import com.safezone.nodesecure.platform.alerts.application.commands.MarkAlertAsFalseAlarmCommand;
import com.safezone.nodesecure.platform.alerts.application.commands.ReconcileAlertCommand;
import com.safezone.nodesecure.platform.alerts.application.queries.GetAllSecurityAlertsQuery;
import com.safezone.nodesecure.platform.alerts.application.queries.GetSecurityAlertByIdQuery;
import com.safezone.nodesecure.platform.alerts.application.queryservices.SecurityAlertQueryService;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.resources.MarkAlertAsFalseAlarmResource;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.resources.RaiseSecurityAlertResource;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.resources.ReconcileAlertResource;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.resources.SecurityAlertResource;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.transform.AlertValueObjectMapper;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.transform.AlertProblemResponseMapper;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.transform.RaiseSecurityAlertCommandMapper;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.transform.SecurityAlertCommandHttpResponseMapper;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.transform.SecurityAlertResourceMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Inbound service in the interface layer for the Alerts bounded context.
 * POST operations delegate to {@link SecurityAlertCommandService}; GET operations to {@link SecurityAlertQueryService}.
 *
 * @since 1.0
 */
@RestController
@RequestMapping(value = "/api/v1/security-alerts", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Security Alerts", description = "Security alert endpoints: raise, list and close alerts (US03, US07)")
public class SecurityAlertsController {
    private static final Logger log = LoggerFactory.getLogger(SecurityAlertsController.class);

    private final SecurityAlertCommandService commandService;
    private final SecurityAlertQueryService queryService;
    private final MessageSource messageSource;

    public SecurityAlertsController(SecurityAlertCommandService commandService,
                                    SecurityAlertQueryService queryService,
                                    MessageSource messageSource) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.messageSource = messageSource;
    }

    /**
     * Raises a new open alert.
     *
     * @param resource request payload
     * @return the created alert
     */
    @Operation(
            summary = "Raise a security alert",
            description = "Creates an OPEN alert (discrepancy, unauthorized access or node disconnection). "
                    + "When no severity is sent, the default of the alert type is used (unauthorized access is CRITICAL).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Alert raised",
                    content = @Content(schema = @Schema(implementation = SecurityAlertResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> raiseSecurityAlert(@Valid @RequestBody RaiseSecurityAlertResource resource) {
        log.debug("POST /api/v1/security-alerts - warehouseId={}, type={}", resource.warehouseId(), resource.alertType());
        var result = commandService.handle(RaiseSecurityAlertCommandMapper.toCommandFromResource(resource));
        return SecurityAlertCommandHttpResponseMapper.toResponseEntityFromResult(result, messageSource, HttpStatus.CREATED);
    }

    /**
     * Lists security alerts, newest first.
     *
     * @param status      optional status filter
     * @param severity    optional severity filter
     * @param warehouseId optional warehouse filter
     * @return list of alerts
     */
    @Operation(
            summary = "Get all security alerts",
            description = "Lists alerts, newest first. Filter by status (e.g. OPEN for the dashboard), severity or warehouse.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alerts retrieved",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = SecurityAlertResource.class)))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<List<SecurityAlertResource>> getAllSecurityAlerts(
            @Parameter(description = "Only alerts with this status", schema = @Schema(allowableValues = {"OPEN", "RECONCILED", "FALSE_ALARM"}))
            @RequestParam(name = "status", required = false) String status,
            @Parameter(description = "Only alerts with this severity", schema = @Schema(allowableValues = {"MINOR", "MAJOR", "CRITICAL"}))
            @RequestParam(name = "severity", required = false) String severity,
            @Parameter(description = "Only alerts of this warehouse", example = "1")
            @RequestParam(name = "warehouseId", required = false) Long warehouseId) {
        log.debug("GET /api/v1/security-alerts - status={}, severity={}, warehouseId={}", status, severity, warehouseId);
        var query = new GetAllSecurityAlertsQuery(
                AlertValueObjectMapper.toOptionalStatusFromString(status),
                AlertValueObjectMapper.toOptionalSeverityFromString(severity),
                warehouseId);
        var alerts = queryService.handle(query);
        return ResponseEntity.ok(alerts.stream().map(SecurityAlertResourceMapper::toResourceFromEntity).toList());
    }

    /**
     * Gets an alert by id.
     *
     * @param id alert identifier
     * @return the alert, or 404 when it does not exist
     */
    @Operation(summary = "Get a security alert by ID", description = "Gets one security alert.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alert found",
                    content = @Content(schema = @Schema(implementation = SecurityAlertResource.class))),
            @ApiResponse(responseCode = "404", description = "Alert not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("{id}")
    public ResponseEntity<?> getSecurityAlertById(
            @Parameter(description = "Alert identifier", example = "1", required = true)
            @PathVariable Long id) {
        log.debug("GET /api/v1/security-alerts/{}", id);
        var alert = queryService.handle(new GetSecurityAlertByIdQuery(id));
        if (alert.isEmpty()) {
            return AlertProblemResponseMapper.notFound(messageSource, "alert.error.notFoundById", id);
        }
        return ResponseEntity.ok(SecurityAlertResourceMapper.toResourceFromEntity(alert.get()));
    }

    /**
     * Marks an open alert as a false alarm.
     *
     * @param id       alert identifier
     * @param resource request payload with the mandatory justification
     * @return the closed alert
     */
    @Operation(
            summary = "Mark an alert as a false alarm",
            description = "Closes an OPEN alert as FALSE_ALARM. A justification is required because the stock movement was authorized (US07).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alert closed as false alarm",
                    content = @Content(schema = @Schema(implementation = SecurityAlertResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - missing justification",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Alert not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - the alert is already closed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("{id}/false-alarm")
    public ResponseEntity<?> markAlertAsFalseAlarm(
            @Parameter(description = "Alert identifier", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody MarkAlertAsFalseAlarmResource resource) {
        log.debug("POST /api/v1/security-alerts/{}/false-alarm", id);
        var result = commandService.handle(new MarkAlertAsFalseAlarmCommand(id, resource.justification(), resource.resolvedBy()));
        return SecurityAlertCommandHttpResponseMapper.toResponseEntityFromResult(result, messageSource, HttpStatus.OK);
    }

    /**
     * Marks an open alert as reconciled.
     *
     * @param id       alert identifier
     * @param resource request payload
     * @return the closed alert
     */
    @Operation(
            summary = "Mark an alert as reconciled",
            description = "Closes an OPEN alert as RECONCILED once the physical event matches the inventory.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Alert closed as reconciled",
                    content = @Content(schema = @Schema(implementation = SecurityAlertResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Alert not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - the alert is already closed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("{id}/reconciliation")
    public ResponseEntity<?> reconcileAlert(
            @Parameter(description = "Alert identifier", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ReconcileAlertResource resource) {
        log.debug("POST /api/v1/security-alerts/{}/reconciliation", id);
        var result = commandService.handle(new ReconcileAlertCommand(id, resource.justification(), resource.resolvedBy()));
        return SecurityAlertCommandHttpResponseMapper.toResponseEntityFromResult(result, messageSource, HttpStatus.OK);
    }
}
