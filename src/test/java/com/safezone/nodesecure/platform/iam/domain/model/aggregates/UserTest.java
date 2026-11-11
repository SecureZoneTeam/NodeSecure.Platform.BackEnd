package com.safezone.nodesecure.platform.iam.domain.model.aggregates;

import com.safezone.nodesecure.platform.iam.domain.model.events.UserRegisteredEvent;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    private User newUser() {
        return User.create(new Username("fperez"), new EmailAddress("fernando.perez@safezone.pe"),
                "Fernando Pérez", "hash", Set.of(Role.ROLE_ADMINISTRATOR), 1L, 4L, NOW);
    }

    @Test
    void createRegistersEventOnlyOnce() {
        User user = newUser();

        assertNull(user.getId());
        List<DomainEvent> events = user.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof UserRegisteredEvent);
        assertTrue(user.pullDomainEvents().isEmpty());
    }

    @Test
    void restoreDoesNotRegisterEvents() {
        User user = User.restore(1L, new Username("fperez"), new EmailAddress("fernando.perez@safezone.pe"),
                "Fernando Pérez", "hash", Set.of(Role.ROLE_ADMINISTRATOR), 1L, 4L, NOW, NOW);

        assertEquals(1L, user.getId());
        assertTrue(user.pullDomainEvents().isEmpty());
    }

    @Test
    void userNeedsAtLeastOneRole() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () ->
                User.create(new Username("fperez"), new EmailAddress("fernando.perez@safezone.pe"),
                        "Fernando Pérez", "hash", Set.of(), 1L, 4L, NOW));
        assertEquals("iam.error.roles.notEmpty", error.getMessage());
    }

    @Test
    void updateChangesOnlyGivenFields() {
        User user = newUser();
        Instant later = NOW.plusSeconds(60);

        user.update(null, "Fernando Sebastián Pérez", Set.of(Role.ROLE_AUDITOR), null, later);

        assertEquals("fernando.perez@safezone.pe", user.getEmail().value());
        assertEquals("Fernando Sebastián Pérez", user.getFullName());
        assertTrue(user.hasRole(Role.ROLE_AUDITOR));
        assertFalse(user.hasRole(Role.ROLE_ADMINISTRATOR));
        assertEquals(4L, user.getWarehouseId());
        assertEquals(later, user.getUpdatedAt());
    }

    @Test
    void roleNamesAreSorted() {
        User user = User.create(new Username("lanahua"), new EmailAddress("liz.anahua@safezone.pe"),
                "Liz Anahua", "hash", Set.of(Role.ROLE_AUDITOR, Role.ROLE_ADMINISTRATOR), 1L, null, NOW);

        assertEquals(List.of("ROLE_ADMINISTRATOR", "ROLE_AUDITOR"), user.getRoleNames());
    }
}
