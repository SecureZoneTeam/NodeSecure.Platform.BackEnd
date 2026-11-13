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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StockMovementCommandServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    private final Map<Long, Product> products = new HashMap<>();
    private final List<StockMovement> movements = new ArrayList<>();
    private final List<Object> publishedEvents = new ArrayList<>();
    private StockMovementCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        products.clear();
        movements.clear();
        publishedEvents.clear();
        products.put(1L, Product.rehydrate(1L, 1L, "Router", new Sku("NET-ROUTER"), ProductCategory.NETWORKING,
                "UNIT", 10, NOW, NOW, "fperez"));

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
                var saved = StockMovement.rehydrate((long) movements.size() + 1, movement.getProductId(),
                        movement.getWarehouseId(), movement.getType(), movement.getQuantity(),
                        movement.getPreviousStock(), movement.getResultingStock(), movement.getPerformedBy(),
                        movement.getOccurredAt(), movement.getJustification());
                movements.add(saved);
                return saved;
            }
            public Optional<StockMovement> findById(Long id) { return movements.stream().filter(m -> m.getId().equals(id)).findFirst(); }
            public List<StockMovement> findAll(Long productId, Long warehouseId, StockMovementType type) { return movements; }
        };
        service = new StockMovementCommandServiceImpl(productRepository, movementRepository,
                publishedEvents::add, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void entryIncreasesStockAndRecordsMovement() {
        var result = service.handle(new RegisterStockMovementCommand(1L, 1L, StockMovementType.ENTRY, 5, "fperez", null));
        var movement = result.success().orElseThrow();
        assertEquals(10, movement.getPreviousStock());
        assertEquals(15, movement.getResultingStock());
        assertEquals(15, products.get(1L).getCurrentStock());
        assertEquals(NOW, movement.getOccurredAt());
        assertEquals(1, publishedEvents.size());
    }

    @Test
    void exitDecreasesStock() {
        var result = service.handle(new RegisterStockMovementCommand(1L, 1L, StockMovementType.EXIT, 4, "ksandoval", null));
        assertEquals(6, result.success().orElseThrow().getResultingStock());
        assertEquals(6, products.get(1L).getCurrentStock());
    }

    @Test
    void exitBeyondStockFailsWithoutChangingAnything() {
        var result = service.handle(new RegisterStockMovementCommand(1L, 1L, StockMovementType.EXIT, 11, "ksandoval", null));
        var failure = result.failure().orElseThrow();
        assertInstanceOf(StockMovementCommandFailure.InsufficientStock.class, failure);
        assertEquals(10, products.get(1L).getCurrentStock());
        assertTrue(movements.isEmpty());
        assertTrue(publishedEvents.isEmpty());
    }

    @Test
    void reconciliationReplacesStockWithCountedQuantity() {
        var result = service.handle(new RegisterStockMovementCommand(1L, 1L, StockMovementType.RECONCILIATION, 7,
                "lravello", "Physical count after the alert"));
        var movement = result.success().orElseThrow();
        assertEquals(10, movement.getPreviousStock());
        assertEquals(7, movement.getResultingStock());
        assertEquals(7, products.get(1L).getCurrentStock());
    }

    @Test
    void unknownProductFails() {
        var result = service.handle(new RegisterStockMovementCommand(99L, 1L, StockMovementType.ENTRY, 1, "fperez", null));
        assertInstanceOf(StockMovementCommandFailure.ProductNotFound.class, result.failure().orElseThrow());
    }

    @Test
    void reconciliationWithoutJustificationIsRejectedAndNothingIsSaved() {
        assertThrows(IllegalArgumentException.class, () -> service.handle(
                new RegisterStockMovementCommand(1L, 1L, StockMovementType.RECONCILIATION, 7, "lravello", null)));
        // The exception rolls the transaction back, so no movement is stored.
        assertTrue(movements.isEmpty());
        assertTrue(publishedEvents.isEmpty());
    }
}
