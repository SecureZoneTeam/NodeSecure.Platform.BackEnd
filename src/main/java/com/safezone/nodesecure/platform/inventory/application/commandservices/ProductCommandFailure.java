package com.safezone.nodesecure.platform.inventory.application.commandservices;

public sealed interface ProductCommandFailure
        permits ProductCommandFailure.DuplicateSku, ProductCommandFailure.NotFound {

    String messageKey();

    default Object[] messageArgs() {
        return new Object[0];
    }

    record DuplicateSku() implements ProductCommandFailure {
        @Override
        public String messageKey() {
            return "product.error.duplicateSku";
        }
    }

    record NotFound(Long id) implements ProductCommandFailure {
        @Override
        public String messageKey() {
            return "product.error.notFoundById";
        }

        @Override
        public Object[] messageArgs() {
            return new Object[]{id};
        }
    }
}
