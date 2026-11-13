package com.safezone.nodesecure.platform.inventory.application.commandservices;

public sealed interface StockMovementCommandFailure
        permits StockMovementCommandFailure.ProductNotFound, StockMovementCommandFailure.InsufficientStock {

    String messageKey();

    default Object[] messageArgs() {
        return new Object[0];
    }

    record ProductNotFound(Long productId) implements StockMovementCommandFailure {
        @Override
        public String messageKey() {
            return "stock.error.productNotFound";
        }

        @Override
        public Object[] messageArgs() {
            return new Object[]{productId};
        }
    }

    record InsufficientStock(int requested, int available) implements StockMovementCommandFailure {
        @Override
        public String messageKey() {
            return "stock.error.insufficient";
        }

        @Override
        public Object[] messageArgs() {
            return new Object[]{requested, available};
        }
    }
}
