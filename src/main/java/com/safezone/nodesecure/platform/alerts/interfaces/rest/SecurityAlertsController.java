package com.safezone.nodesecure.platform.alerts.interfaces.rest;

import com.safezone.nodesecure.platform.alerts.application.commands.MarkAlertAsFalseAlarmCommand;
import com.safezone.nodesecure.platform.alerts.application.commands.ReconcileAlertCommand;
import com.safezone.nodesecure.platform.alerts.application.commandservices.SecurityAlertCommandService;
import com.safezone.nodesecure.platform.alerts.application.queries.GetAllSecurityAlertsQuery;
import com.safezone.nodesecure.platform.alerts.application.queries.GetSecurityAlertByIdQuery;
import com.safezone.nodesecure.platform.alerts.application.queryservices.SecurityAlertQueryService;
import com.safezone.nodesecure.platform.alerts.domain.model.aggregates.SecurityAlert;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.resources.MarkAlertAsFalseAlarmResource;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.resources.RaiseSecurityAlertResource;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.resources.ReconcileAlertResource;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.resources.SecurityAlertResource;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.transform.AlertProblemResponseMapper;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.transform.AlertValueObjectMapper;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.transform.RaiseSecurityAlertCommandMapper;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.transform.SecurityAlertCommandHttpResponseMapper;
import com.safezone.nodesecure.platform.alerts.interfaces.rest.transform.SecurityAlertResourceMapper;
import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseContextFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * Security alerts. Every alert belongs to a warehouse, and warehouses belong to exactly one organization:
 * the user only reaches alerts of the warehouses of its organization that are inside its scope.
 * The person who closes an alert is always the signed-in user.
 */
@RestController
@RequestMapping(value = "/api/v1/security-alerts", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Security Alerts", description = "Security alert endpoints: raise, list and close alerts (US03, US07)")
public class SecurityAlertsController {

    private final SecurityAlertCommandService commandService;
    private final SecurityAlertQueryService queryService;
    private final IamContextFacade iamFacade;
    private final WarehouseContextFacade warehouseFacade;
    private final MessageSource messageSource;

    public SecurityAlertsController(SecurityAlertCommandService commandService,
                                    SecurityAlertQueryService queryService,
                                    IamContextFacade iamFacade,
                                    WarehouseContextFacade warehouseFacade,
                                    MessageSource messageSource) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.iamFacade = iamFacade;
        this.warehouseFacade = warehouseFacade;
        this.messageSource = messageSource;
    }

    @Operation(summary = "Raise a security alert",
            description = "Creates an OPEN alert for a warehouse of the organization assigned to the user.")
    @PreAuthorize("hasAuthority('alerts.manage')")
    @PostMapping
    public ResponseEntity<?> raiseSecurityAlert(@Valid @RequestBody RaiseSecurityAlertResource resource) {
        if (!visibleWarehouseIds().contains(resource.warehouseId())) {
            return AlertProblemResponseMapper.notFound(messageSource, "alert.error.warehouseNotFound",
                    resource.warehouseId());
        }
        if (resource.zoneId() != null && warehouseFacade.findZoneConfiguration(resource.zoneId(), iamFacade.requireCompanyId())
                .map(zone -> !Objects.equals(zone.warehouseId(), resource.warehouseId()))
                .orElse(true)) {
            return AlertProblemResponseMapper.notFound(messageSource, "alert.error.zoneNotFound", resource.zoneId());
        }
        var result = commandService.handle(RaiseSecurityAlertCommandMapper.toCommandFromResource(resource));
        return SecurityAlertCommandHttpResponseMapper.toResponseEntityFromResult(result, messageSource, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all security alerts",
            description = "Alerts of the user's warehouses, newest first. Filter by status, severity or warehouse.")
    @PreAuthorize("hasAuthority('alerts.view')")
    @GetMapping
    public ResponseEntity<List<SecurityAlertResource>> getAllSecurityAlerts(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "severity", required = false) String severity,
            @RequestParam(name = "warehouseId", required = false) Long warehouseId) {
        var query = new GetAllSecurityAlertsQuery(
                AlertValueObjectMapper.toOptionalStatusFromString(status),
                AlertValueObjectMapper.toOptionalSeverityFromString(severity),
                warehouseId,
                visibleWarehouseIds());
        var alerts = queryService.handle(query);
        return ResponseEntity.ok(alerts.stream().map(SecurityAlertResourceMapper::toResourceFromEntity).toList());
    }

    @Operation(summary = "Get an alert by id")
    @PreAuthorize("hasAuthority('alerts.view')")
    @GetMapping("{id}")
    public ResponseEntity<?> getSecurityAlertById(@PathVariable Long id) {
        return findVisible(id)
                .<ResponseEntity<?>>map(alert -> ResponseEntity.ok(SecurityAlertResourceMapper.toResourceFromEntity(alert)))
                .orElseGet(() -> AlertProblemResponseMapper.notFound(messageSource, "alert.error.notFoundById", id));
    }

    @Operation(summary = "Mark an alert as a false alarm",
            description = "Closes an OPEN alert as FALSE_ALARM. A justification is required (US07).")
    @PreAuthorize("hasAuthority('alerts.manage')")
    @PostMapping("{id}/false-alarm")
    public ResponseEntity<?> markAlertAsFalseAlarm(@PathVariable Long id,
                                                   @Valid @RequestBody MarkAlertAsFalseAlarmResource resource) {
        if (findVisible(id).isEmpty()) {
            return AlertProblemResponseMapper.notFound(messageSource, "alert.error.notFoundById", id);
        }
        var result = commandService.handle(new MarkAlertAsFalseAlarmCommand(id, resource.justification(),
                iamFacade.currentUsername()));
        return SecurityAlertCommandHttpResponseMapper.toResponseEntityFromResult(result, messageSource, HttpStatus.OK);
    }

    @Operation(summary = "Mark an alert as reconciled",
            description = "Closes an OPEN alert as RECONCILED once the physical event matches the inventory.")
    @PreAuthorize("hasAuthority('alerts.manage')")
    @PostMapping("{id}/reconciliation")
    public ResponseEntity<?> reconcileAlert(@PathVariable Long id, @Valid @RequestBody ReconcileAlertResource resource) {
        if (findVisible(id).isEmpty()) {
            return AlertProblemResponseMapper.notFound(messageSource, "alert.error.notFoundById", id);
        }
        var result = commandService.handle(new ReconcileAlertCommand(id, resource.justification(),
                iamFacade.currentUsername()));
        return SecurityAlertCommandHttpResponseMapper.toResponseEntityFromResult(result, messageSource, HttpStatus.OK);
    }

    private Optional<SecurityAlert> findVisible(Long id) {
        Set<Long> visible = visibleWarehouseIds();
        return queryService.handle(new GetSecurityAlertByIdQuery(id))
                .filter(alert -> visible.contains(alert.getWarehouseId()));
    }

    /** Warehouses of the organization intersected with the user's scope. */
    private Set<Long> visibleWarehouseIds() {
        Set<Long> organizationWarehouses = warehouseFacade.findWarehouseIdsByCompanyId(iamFacade.requireCompanyId());
        Set<Long> scope = iamFacade.warehouseScope();
        if (scope == null) {
            return organizationWarehouses;
        }
        return organizationWarehouses.stream().filter(scope::contains).collect(Collectors.toUnmodifiableSet());
    }
}
