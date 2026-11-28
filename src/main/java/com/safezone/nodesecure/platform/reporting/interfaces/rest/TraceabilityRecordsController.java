package com.safezone.nodesecure.platform.reporting.interfaces.rest;

import com.safezone.nodesecure.platform.reporting.application.internal.services.TraceabilityQueryService;
import com.safezone.nodesecure.platform.reporting.domain.model.aggregates.TraceabilityRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/**
 * T29 + T30: read-only access to the tamper-proof traceability log. There is intentionally no
 * POST, PUT, PATCH or DELETE: records are written only by the platform from domain events.
 */
@RestController
@RequestMapping(value = "/api/v1/traceability-records", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Traceability", description = "Tamper-proof log of users, sensors, inventory and alerts events (read only)")
public class TraceabilityRecordsController {

    private final TraceabilityQueryService queryService;

    public TraceabilityRecordsController(TraceabilityQueryService queryService) {
        this.queryService = queryService;
    }

    public record TraceabilityRecordResource(Long id, Long companyId, Long warehouseId, Long zoneId, String recordType,
                                             String eventType, String entityType, String entityId, String actor,
                                             String description, Instant occurredAt, Instant recordedAt) {
        static TraceabilityRecordResource from(TraceabilityRecord record) {
            return new TraceabilityRecordResource(record.getId(), record.getCompanyId(), record.getWarehouseId(),
                    record.getZoneId(), record.getRecordType(), record.getEventType(), record.getEntityType(),
                    record.getEntityId(), record.getActor(), record.getDescription(), record.getOccurredAt(),
                    record.getRecordedAt());
        }
    }

    public record PageResource<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
        static PageResource<TraceabilityRecordResource> from(Page<TraceabilityRecord> page) {
            return new PageResource<>(page.getContent().stream().map(TraceabilityRecordResource::from).toList(),
                    page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
        }
    }

    @Operation(summary = "Search the traceability log of my organization",
            description = "All filters are optional and combinable. Workers only see records of their warehouses. "
                    + "Dates are ISO-8601 instants (e.g. 2026-11-01T00:00:00Z); 'from' is inclusive and 'to' exclusive.")
    @ApiResponse(responseCode = "200", description = "Page of records, newest first by default")
    @ApiResponse(responseCode = "400", description = "Invalid record type, date range or page")
    @ApiResponse(responseCode = "403", description = "Warehouse outside of the user's scope")
    @ApiResponse(responseCode = "404", description = "Warehouse not found in my organization")
    @PreAuthorize("hasAuthority('traceability.view')")
    @GetMapping
    public ResponseEntity<PageResource<TraceabilityRecordResource>> search(
            @RequestParam(required = false) Long warehouseId,
            @Parameter(description = "One or more of PHYSICAL_ACCESS, NODE_DISCONNECTION, INVENTORY_MOVEMENT, DISCREPANCY, "
                    + "SECURITY_ALERT, PRODUCT_CATALOG, USER_MANAGEMENT, SENSOR_MANAGEMENT")
            @RequestParam(name = "recordType", required = false) List<String> recordTypes,
            @Parameter(description = "Exact event, e.g. STOCK_RECONCILIATION or SECURITY_ALERT_RAISED")
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "asc or desc (default) by occurrence time")
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok(PageResource.from(queryService.search(new TraceabilityQueryService.Filter(
                warehouseId, recordTypes, eventType, from, to, page, size, "asc".equalsIgnoreCase(direction)))));
    }

    @Operation(summary = "Get a traceability record of my organization")
    @ApiResponse(responseCode = "404", description = "Not found or not visible for the user")
    @PreAuthorize("hasAuthority('traceability.view')")
    @GetMapping("/{id}")
    public ResponseEntity<TraceabilityRecordResource> getById(@PathVariable Long id) {
        return ResponseEntity.ok(TraceabilityRecordResource.from(queryService.getById(id)));
    }
}
