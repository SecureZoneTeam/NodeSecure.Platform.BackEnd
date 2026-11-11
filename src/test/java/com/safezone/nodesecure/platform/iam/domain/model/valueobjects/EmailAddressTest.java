package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailAddressTest {

    @Test
    void savesEmailInLowercase() {
        assertEquals("fernando.perez@safezone.pe", new EmailAddress("Fernando.Perez@SafeZone.pe").value());
    }

    @Test
    void rejectsEmailWithoutAt() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new EmailAddress("fperez.safezone.pe"));
        assertEquals("iam.error.email.invalid", error.getMessage());
    }

    @Test
    void rejectsBlankEmail() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new EmailAddress(""));
        assertEquals("iam.error.email.notBlank", error.getMessage());
    }
}
