package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoleTest {

    @Test
    void readsRoleFromText() {
        assertEquals(Role.ROLE_AUDITOR, Role.from(" role_auditor "));
    }

    @Test
    void rejectsUnknownRole() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> Role.from("ROLE_BOSS"));
        assertEquals("iam.error.role.invalid", error.getMessage());
    }
}
