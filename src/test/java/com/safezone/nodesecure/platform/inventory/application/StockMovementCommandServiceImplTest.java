package com.safezone.nodesecure.platform.inventory.application;

import com.safezone.nodesecure.platform.inventory.application.commandservices.StockMovementCommandFailure;
import com.safezone.nodesecure.platform.inventory.application.commands.RegisterStockMovementCommand;
import com.safezone.nodesecure.platform.inventory.application.internal.commandservices.StockMovementCommandServiceImpl;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.Product;
import com.safezone.nodesecure.platform.inventory.domain.model.aggregates.StockMovement;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.ProductCategory;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.Sku;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;
import com.safezone.nodesecure.platform.inventory.domain.repositories.ProductRepository;
import com.safezone.nodesecure.platform.inventory.domain.repositories.StockMovementRepository;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseContextFacade;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseZoneConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StockMovementCommandServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");
    private static final Long COMPANY = 1L;
    private static final Long OTHER_COMPANY = 2L;

    private final Map<Long, Product> products = new HashMap<>();
    private final List<StockMovement> movements = new ArrayList<>();
    private final List<Object> publishedEvents = new ArrayList<>();
    /** warehouseId -> companyId */
    private final Map<Long, Long> warehouses = Map.of(1L, COMPANY, 3L, OTHER_COMPANY);
    private StockMovementCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        products.clear();
        movements.clear();
        publishedEvents.clear();
        products.put(1L, Product.rehydrate(1L, COMPANY, "Router", new Sku("NET-ROUTER"), ProductCategory.NETWORKING,
                "UNIT", 10, NOW, NOW, "fperez"));
        products.put(2L, Product.rehydrate(2L, OTHER_COMPANY, "Switch", new Sku("NET-SWITCH"), ProductCategory.NETWORKING,
                "UNIT", 10, NOW, NOW, "owner-b"));

        ProductRepository productRepository = new ProductRepository() {
            public Product save(Product product) { products.put(product.getId(), product); return product; }
            public Optional<Product> findById(Long id) { return Optional.ofNullable(products.get(id)); }
            public Optional<Product> findByIdForUpdate(Long id) { return findById(id); }
            public List<Product> findAll() { return List.copyOf(products.values()); }
            public List<Product> findAllByCompanyId(Long companyId) { return findAll(); }
            public boolean existsByCompanyIdAndSku(Long companyId, Sku sku) { return false; }
            public boolean existsByCompanyIdAndSkuAndIdNot(Long companyId, Sku sku, Long id) { return false; }
        };
        StockMovementRepository movementRepository = new StockMovementRepository() {
            public StockMovement save(StockMovement movement) {
                var saved = StockMovement.rehydrate((long) movements.size() + 1, movement.getCompanyId(),
                        movement.getProductId(), movement.getWarehouseId(), movement.getType(), movement.getQuantity(),
                        movement.getPreviousStock(), movement.getResultingStock(), movement.getPerformedBy(),
                        movement.getOccurredAt(), movement.getJustification());
                movements.add(saved);
                return saved;
            }
            public Optional<StockMovement> findById(Long id) { return movements.stream().filter(m -> m.getId().equals(id)).findFirst(); }
            public List<StockMovement> findAll(Long companyId, Long productId, Long warehouseId, StockMovementType type,
                                               Collection<Long> warehouseScope) { return movements; }
        };
        WarehouseContextFacade warehouseFacade = new WarehouseContextFacade() {
            public boolean zoneBelongsToCompany(Long zoneId, Long companyId) { return false; }
            public Optional<WarehouseZoneConfiguration> findZoneConfiguration(Long zoneId, Long companyId) { return Optional.empty(); }
            public Set<Long> findZoneIdsByWarehouseId(Long warehouseId, Long companyId) { return Set.of(); }
            public Set<Long> findZoneIdsByWarehouseIds(Collection<Long> warehouseIds, Long companyId) { return Set.of(); }
            public boolean warehouseBelongsToCompany(Long warehouseId, Long companyId) {
                return companyId != null && companyId.equals(warehouses.get(warehouseId));
            }
            public Set<Long> findWarehouseIdsByCompanyId(Long companyId) { return Set.of(); }
            public boolean allWarehousesBelongToCompany(Collection<Long> ids, Long companyId) { return false; }
            @Override
            public Optional<String> findWarehouseName(Long warehouseId, Long companyId) { return Optional.empty(); }
        };
        service = new StockMovementCommandServiceImpl(productRepository, movementRepository, warehouseFacade,
                publishedEvents::add, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private RegisterStockMovementCommand command(Long productId, Long warehouseId, StockMovementType type, int quantity,
                                                 String justification) {
        return new RegisterStockMovementCommand(COMPANY, productId, warehouseId, type, quantity, "fperez", justification);
    }

    @Test
    void entryIncreasesStockAndRecordsMovementOfTheOrganization() {
        var movement = service.handle(command(1L, 1L, StockMovementType.ENTRY, 5, null)).success().orElseThrow();
        assertEquals(10, movement.getPreviousStock());
        assertEquals(15, movement.getResultingStock());
        assertEquals(COMPANY, movement.getCompanyId());
        assertEquals(15, products.get(1L).getCurrentStock());
        assertEquals(NOW, movement.getOccurredAt());
        assertEquals(1, publishedEvents.size());
    }

    @Test
    void exitDecreasesStock() {
        var result = service.handle(command(1L, 1L, StockMovementType.EXIT, 4, null));
        assertEquals(6, result.success().orElseThrow().getResultingStock());
        assertEquals(6, products.get(1L).getCurrentStock());
    }

    @Test
    void exitBeyondStockFailsWithoutChangingAnything() {
        var failure = service.handle(command(1L, 1L, StockMovementType.EXIT, 11, null)).failure().orElseThrow();
        assertInstanceOf(StockMovementCommandFailure.InsufficientStock.class, failure);
        assertEquals(10, products.get(1L).getCurrentStock());
        assertTrue(movements.isEmpty());
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    void reconciliationReplacesStockWithCountedQuantity() {
        var movement = service.handle(command(1L, 1L, StockMovementType.RECONCILIATION, 7,
                "Physical count after the alert")).success().orElseThrow();
        assertEquals(10, movement.getPreviousStock());
        assertEquals(7, movement.getResultingStock());
        assertEquals(7, products.get(1L).getCurrentStock());
    }

    @Test
    void unknownProductFails() {
        var result = service.handle(command(99L, 1L, StockMovementType.ENTRY, 1, null));
        assertInstanceOf(StockMovementCommandFailure.ProductNotFound.class, result.failure().orElseThrow());
    }

    @Test
    void productOfAnotherOrganizationBehavesAsMissingAndIsNotTouched() {
        var result = service.handle(command(2L, 1L, StockMovementType.EXIT, 1, null));
        assertInstanceOf(StockMovementCommandFailure.ProductNotFound.class, result.failure().orElseThrow());
        assertEquals(10, products.get(2L).getCurrentStock());
        assertTrue(movements.isEmpty());
    }

    @Test
    void warehouseOfAnotherOrganizationIsRejected() {
        var result = service.handle(command(1L, 3L, StockMovementType.ENTRY, 1, null));
        assertInstanceOf(StockMovementCommandFailure.WarehouseNotFound.class, result.failure().orElseThrow());
        assertEquals(10, products.get(1L).getCurrentStock());
        assertTrue(movements.isEmpty());
    }

    @Test
    void reconciliationWithoutJustificationIsRejectedAndNothingIsSaved() {
        assertThrows(IllegalArgumentException.class, () -> service.handle(
                command(1L, 1L, StockMovementType.RECONCILIATION, 7, null)));
        // The exception rolls the transaction back, so no movement is stored.
        assertTrue(movements.isEmpty());
        assertTrue(publishedEvents.isEmpty());
    }
}
