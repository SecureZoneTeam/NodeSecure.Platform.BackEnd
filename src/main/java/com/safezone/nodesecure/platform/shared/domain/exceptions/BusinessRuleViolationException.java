package com.safezone.nodesecure.platform.shared.domain.exceptions;

/** A request conflicts with the current state of a resource or with a business rule (HTTP 409). */
public class BusinessRuleViolationException extends RuntimeException {

    private final transient Object[] arguments;

    public BusinessRuleViolationException(String messageKey, Object... arguments) {
        super(messageKey);
        this.arguments = arguments;
    }

    public Object[] getArguments() {
        return arguments;
    }
}
