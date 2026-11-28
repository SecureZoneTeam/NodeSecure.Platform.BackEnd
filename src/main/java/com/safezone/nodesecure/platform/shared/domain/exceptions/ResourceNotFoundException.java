package com.safezone.nodesecure.platform.shared.domain.exceptions;

/** A resource does not exist or does not belong to the caller's organization (HTTP 404). */
public class ResourceNotFoundException extends RuntimeException {

    private final transient Object[] arguments;

    public ResourceNotFoundException(String messageKey, Object... arguments) {
        super(messageKey);
        this.arguments = arguments;
    }

    public Object[] getArguments() {
        return arguments;
    }
}
