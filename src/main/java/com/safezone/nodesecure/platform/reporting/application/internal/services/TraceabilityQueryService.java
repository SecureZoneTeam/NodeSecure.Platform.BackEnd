package com.safezone.nodesecure.platform.reporting.application.internal.services;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.reporting.domain.model.aggregates.TraceabilityRecord;
import com.safezone.nodesecure.platform.reporting.domain.model.valueobjects.TraceabilityRecordType;
import com.safezone.nodesecure.platform.reporting.domain.repositories.TraceabilityRecordRepository;
import com.safezone.nodesecure.platform.shared.domain.exceptions.ResourceNotFoundException;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseContextFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * T30: consultation of the traceability log with combinable filters and pagination.
 * Organization and warehouse scope always come from the session: owners see every record of
 * their organization; workers only the records of their assigned warehouses.
 */
@Service
public class TraceabilityQueryService {

    public static final int MAX_PAGE_SIZE = 100;
    private static final List<Long> NO_IDS = List.of(-1L);
    private static final List<String> NO_TYPES = List.of("-");

    private final TraceabilityRecordRepository repository;
    private final IamContextFacade iamFacade;
    private final WarehouseContextFacade warehouseFacade;

    public TraceabilityQueryService(TraceabilityRecordRepository repository, IamContextFacade iamFacade,
                                    WarehouseContextFacade warehouseFacade) {
        this.repository = repository;
        this.iamFacade = iamFacade;
        this.warehouseFacade = warehouseFacade;
    }

    public record Filter(Long warehouseId, List<String> recordTypes, String eventType, Instant from, Instant to,
                         int page, int size, boolean ascending) {
    }

    @Transactional(readOnly = true)
    public Page<TraceabilityRecord> search(Filter filter) {
        if (filter.page() < 0 || filter.size() < 1 || filter.size() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("traceability.error.page.invalid");
        }
        if (filter.from() != null && filter.to() != null && !filter.from().isBefore(filter.to())) {
            throw new IllegalArgumentException("traceability.error.range.invalid");
        }
        Long companyId = iamFacade.requireCompanyId();
        if (filter.warehouseId() != null) {
            requireAccessibleWarehouse(filter.warehouseId(), companyId);
        }
        List<String> types = filter.recordTypes() == null ? List.of() : filter.recordTypes().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> TraceabilityRecordType.parse(value).name())
                .distinct().toList();
        Set<Long> scope = iamFacade.warehouseScope();
        boolean restricted = scope != null;
        String eventType = filter.eventType() == null || filter.eventType().isBlank() ? null : filter.eventType().trim();
        Sort.Direction direction = filter.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC;
        return repository.search(companyId, filter.warehouseId(),
                restricted, restricted && !scope.isEmpty() ? scope : NO_IDS,
                !types.isEmpty(), types.isEmpty() ? NO_TYPES : types,
                eventType, filter.from(), filter.to(),
                PageRequest.of(filter.page(), filter.size(), Sort.by(direction, "occurredAt", "id")));
    }

    @Transactional(readOnly = true)
    public TraceabilityRecord getById(Long id) {
        Long companyId = iamFacade.requireCompanyId();
        Set<Long> scope = iamFacade.warehouseScope();
        return repository.findByIdAndCompanyId(id, companyId)
                .filter(record -> scope == null || (record.getWarehouseId() != null && scope.contains(record.getWarehouseId())))
                .orElseThrow(() -> new ResourceNotFoundException("traceability.error.notFound", id));
    }

    /** 404 when the warehouse is not of the organization, 403 when it is outside the worker's scope. */
    public void requireAccessibleWarehouse(Long warehouseId, Long companyId) {
        if (!warehouseFacade.warehouseBelongsToCompany(warehouseId, companyId)) {
            throw new ResourceNotFoundException("traceability.error.warehouseNotFound", warehouseId);
        }
        if (!iamFacade.canAccessWarehouse(warehouseId)) {
            throw new AccessDeniedException("Warehouse outside of the user's scope");
        }
    }
}
