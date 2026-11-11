package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

public record Password(String value) {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 72;

    public Password {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("iam.error.password.notBlank");
        }
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("iam.error.password.size");
        }
    }

    @Override
    public String toString() {
        return "********";
    }
}
