package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordTest {

    @Test
    void acceptsPasswordWithEightCharacters() {
        assertEquals("12345678", new Password("12345678").value());
    }

    @Test
    void rejectsShortPassword() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new Password("1234"));
        assertEquals("iam.error.password.size", error.getMessage());
    }

    @Test
    void hidesPasswordInText() {
        assertEquals("********", new Password("12345678").toString());
    }
}
