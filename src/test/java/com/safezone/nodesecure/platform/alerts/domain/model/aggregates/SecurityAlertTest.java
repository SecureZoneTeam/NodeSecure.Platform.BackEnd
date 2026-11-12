package com.safezone.nodesecure.platform.alerts.domain.model.aggregates;

import com.safezone.nodesecure.platform.alerts.domain.exceptions.AlertAlreadyResolvedException;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertSeverity;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertStatus;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAlertTest {
    private static final Instant NOW = Instant.parse("2026-10-04T02:47:31Z");
    private static final Instant LATER = Instant.parse("2026-10-04T03:00:00Z");

    private SecurityAlert openAlert(Long id) {
        return SecurityAlert.rehydrate(id, 1L, 3L, 2L, AlertType.UNAUTHORIZED_ACCESS, AlertSeverity.CRITICAL,
                AlertStatus.OPEN, "Unauthorized access", NOW, null, null, null);
    }

    @Test
    void raiseUsesDefaultSeverityOfTheType() {
        var critical = SecurityAlert.raise(1L, 3L, 2L, AlertType.UNAUTHORIZED_ACCESS, null, "Access out of shift", NOW);
        assertEquals(AlertSeverity.CRITICAL, critical.getSeverity());
        assertEquals(AlertStatus.OPEN, critical.getStatus());
        assertTrue(critical.isOpen());
        assertEquals(1, critical.pullDomainEvents().size());

        var node = SecurityAlert.raise(1L, null, null, AlertType.NODE_DISCONNECTION, null, "Node offline", NOW);
        assertEquals(AlertSeverity.MAJOR, node.getSeverity());
        var discrepancy = SecurityAlert.raise(1L, null, null, AlertType.DISCREPANCY, AlertSeverity.CRITICAL, "No movement", NOW);
        assertEquals(AlertSeverity.CRITICAL, discrepancy.getSeverity());
    }

    @Test
    void raiseValidatesInput() {
        assertThrows(IllegalArgumentException.class,
                () -> SecurityAlert.raise(0L, null, null, AlertType.DISCREPANCY, null, "msg", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> SecurityAlert.raise(1L, null, null, AlertType.DISCREPANCY, null, " ", NOW));
        assertThrows(IllegalArgumentException.class,
                () -> SecurityAlert.raise(1L, null, null, null, null, "msg", NOW));
    }

    @Test
    void falseAlarmRequiresJustificationAndClosesTheAlert() {
        var alert = openAlert(5L);
        assertThrows(IllegalArgumentException.class, () -> alert.markAsFalseAlarm(" ", "lanahua", LATER));
        assertTrue(alert.isOpen());

        alert.markAsFalseAlarm("Autorizado verbalmente por el supervisor", "lanahua", LATER);
        assertEquals(AlertStatus.FALSE_ALARM, alert.getStatus());
        assertEquals("Autorizado verbalmente por el supervisor", alert.getJustification());
        assertEquals("lanahua", alert.getResolvedBy());
        assertEquals(LATER, alert.getResolvedAt());
        assertEquals(1, alert.pullDomainEvents().size());
    }

    @Test
    void reconcileAllowsMissingJustification() {
        var alert = openAlert(5L);
        alert.reconcile(null, "lanahua", LATER);
        assertEquals(AlertStatus.RECONCILED, alert.getStatus());
        assertNull(alert.getJustification());
    }

    @Test
    void closedAlertCannotBeClosedAgain() {
        var alert = openAlert(5L);
        alert.reconcile(null, "lanahua", LATER);
        var error = assertThrows(AlertAlreadyResolvedException.class,
                () -> alert.markAsFalseAlarm("Intento tardio de cierre", "lanahua", LATER));
        assertEquals(AlertStatus.RECONCILED, error.getStatus());
        assertEquals(AlertStatus.RECONCILED, alert.getStatus());
    }

    @Test
    void parsingValuesIsCaseInsensitive() {
        assertEquals(AlertStatus.FALSE_ALARM, AlertStatus.fromValue("false_alarm"));
        assertEquals(AlertSeverity.MAJOR, AlertSeverity.fromValue(" major "));
        assertThrows(IllegalArgumentException.class, () -> AlertType.fromValue("FIRE"));
    }
}
