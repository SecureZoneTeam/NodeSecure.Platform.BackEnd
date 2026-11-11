package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UsernameTest {

    @Test
    void savesUsernameInLowercase() {
        assertEquals("fperez", new Username("  FPerez ").value());
    }

    @Test
    void rejectsBlankUsername() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new Username(" "));
        assertEquals("iam.error.username.notBlank", error.getMessage());
    }

    @Test
    void rejectsInvalidCharacters() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new Username("f perez"));
        assertEquals("iam.error.username.invalid", error.getMessage());
    }

    @Test
    void rejectsShortUsername() {
        assertThrows(IllegalArgumentException.class, () -> new Username("fp"));
    }
}
