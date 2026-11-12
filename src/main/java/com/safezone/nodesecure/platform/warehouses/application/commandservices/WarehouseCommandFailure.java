package com.safezone.nodesecure.platform.warehouses.application.commandservices;

public sealed interface WarehouseCommandFailure permits
        WarehouseCommandFailure.NotFound,
        WarehouseCommandFailure.Conflict,
        WarehouseCommandFailure.Validation {

    String messageKey();

    record NotFound(String resource, Long id) implements WarehouseCommandFailure {
        @Override public String messageKey() { return "warehouse.error.notFound"; }
    }

    record Conflict(String messageKey) implements WarehouseCommandFailure {
    }

    record Validation(String messageKey) implements WarehouseCommandFailure {
    }
}
