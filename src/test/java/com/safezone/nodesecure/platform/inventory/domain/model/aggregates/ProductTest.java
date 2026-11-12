package com.safezone.nodesecure.platform.inventory.domain.model.aggregates;

import com.safezone.nodesecure.platform.inventory.domain.exceptions.InsufficientStockException;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.ProductCategory;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.Sku;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {
    private static final Instant NOW = Instant.parse("2026-10-01T09:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-02T09:00:00Z");

    private Product newProduct() {
        return Product.create(1L, "Router Wi-Fi 6", new Sku("NET-ROUTER"), ProductCategory.NETWORKING, null, "fperez", NOW);
    }

    @Test
    void createStartsWithZeroStockDefaultUnitAndOneEvent() {
        var product = newProduct();
        assertNull(product.getId());
        assertEquals(0, product.getCurrentStock());
        assertEquals("UNIT", product.getUnit());
        assertTrue(product.isOutOfStock());
        assertEquals(1, product.pullDomainEvents().size());
        assertTrue(product.pullDomainEvents().isEmpty());
    }

    @Test
    void rehydrationDoesNotEmitEvents() {
        var product = Product.rehydrate(1L, 1L, "Router", new Sku("NET-ROUTER"), ProductCategory.NETWORKING,
                "UNIT", 5, NOW, NOW, null);
        assertTrue(product.pullDomainEvents().isEmpty());
        assertEquals(5, product.getCurrentStock());
    }

    @Test
    void receiveAndDispatchChangeStock() {
        var product = newProduct();
        product.receive(10, LATER);
        product.dispatch(4, LATER);
        assertEquals(6, product.getCurrentStock());
        assertEquals(LATER, product.getUpdatedAt());
        assertTrue(product.canDispatch(6));
        assertFalse(product.canDispatch(7));
    }

    @Test
    void dispatchBeyondStockFails() {
        var product = newProduct();
        product.receive(3, LATER);
        var error = assertThrows(InsufficientStockException.class, () -> product.dispatch(5, LATER));
        assertEquals(5, error.getRequested());
        assertEquals(3, error.getAvailable());
        assertEquals(3, product.getCurrentStock());
    }

    @Test
    void quantitiesMustBePositive() {
        var product = newProduct();
        assertThrows(IllegalArgumentException.class, () -> product.receive(0, LATER));
        assertThrows(IllegalArgumentException.class, () -> product.dispatch(-1, LATER));
    }

    @Test
    void receiveOverflowIsRejected() {
        var product = newProduct();
        product.receive(Integer.MAX_VALUE, LATER);
        assertThrows(IllegalArgumentException.class, () -> product.receive(1, LATER));
    }

    @Test
    void reconcileReplacesStockAndRejectsNegative() {
        var product = newProduct();
        product.receive(10, LATER);
        product.reconcileTo(7, LATER);
        assertEquals(7, product.getCurrentStock());
        product.reconcileTo(0, LATER);
        assertTrue(product.isOutOfStock());
        assertThrows(IllegalArgumentException.class, () -> product.reconcileTo(-1, LATER));
    }

    @Test
    void validatesNameAndCompany() {
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(1L, " ", new Sku("NET-ROUTER"), ProductCategory.OTHER, null, null, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(1L, "ab", new Sku("NET-ROUTER"), ProductCategory.OTHER, null, null, NOW));
        assertThrows(IllegalArgumentException.class,
                () -> Product.create(0L, "Router", new Sku("NET-ROUTER"), ProductCategory.OTHER, null, null, NOW));
    }

    @Test
    void updateDetailsKeepsStock() {
        var product = newProduct();
        product.receive(9, NOW);
        product.updateDetails("Router AX1800", new Sku("net-ax1800"), ProductCategory.SECURITY, "BOX", LATER);
        assertEquals("Router AX1800", product.getName());
        assertEquals("NET-AX1800", product.getSku().value());
        assertEquals(ProductCategory.SECURITY, product.getCategory());
        assertEquals(9, product.getCurrentStock());
    }
}
