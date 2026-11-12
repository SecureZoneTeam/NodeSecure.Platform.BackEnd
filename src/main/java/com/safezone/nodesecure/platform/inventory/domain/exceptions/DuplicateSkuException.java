package com.safezone.nodesecure.platform.inventory.domain.exceptions;

public class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException(Throwable cause) {
        super("Duplicate product SKU", cause);
    }
}
