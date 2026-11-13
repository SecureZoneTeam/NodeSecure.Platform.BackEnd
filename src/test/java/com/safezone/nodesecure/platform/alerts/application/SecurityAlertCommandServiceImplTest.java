package com.safezone.nodesecure.platform.alerts.application;

import com.safezone.nodesecure.platform.alerts.application.commandservices.SecurityAlertCommandFailure;
import com.safezone.nodesecure.platform.alerts.application.commands.MarkAlertAsFalseAlarmCommand;
import com.safezone.nodesecure.platform.alerts.application.commands.RaiseSecurityAlertCommand;
import com.safezone.nodesecure.platform.alerts.application.commands.ReconcileAlertCommand;
import com.safezone.nodesecure.platform.alerts.application.internal.commandservices.SecurityAlertCommandServiceImpl;
import com.safezone.nodesecure.platform.alerts.domain.model.aggregates.SecurityAlert;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertSeverity;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertStatus;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertType;
import com.safezone.nodesecure.platform.alerts.domain.repositories.SecurityAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAlertCommandServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-11-11T10:00:00Z");

    private final Map<Long, SecurityAlert> alerts = new HashMap<>();
    private final List<Object> publishedEvents = new ArrayList<>();
    private SecurityAlertCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        alerts.clear();
        publishedEvents.clear();
        SecurityAlertRepository repository = new SecurityAlertRepository() {
            public SecurityAlert save(SecurityAlert alert) {
                var id = alert.getId() != null ? alert.getId() : (long) alerts.size() + 1;
                var saved = SecurityAlert.rehydrate(id, alert.getWarehouseId(), alert.getZoneId(),
                        alert.getPhysicalEventId(), alert.getAlertType(), alert.getSeverity(), alert.getStatus(),
                        alert.getMessage(), alert.getRaisedAt(), alert.getJustification(), alert.getResolvedAt(),
                        alert.getResolvedBy());
                alerts.put(id, saved);
                return saved;
            }
            public Optional<SecurityAlert> findById(Long id) { return Optional.ofNullable(alerts.get(id)); }
            public Optional<SecurityAlert> findByIdForUpdate(Long id) { return findById(id); }
            public List<SecurityAlert> findAll(AlertStatus status, AlertSeverity severity, Long warehouseId) {
                return List.copyOf(alerts.values());
            }
        };
        service = new SecurityAlertCommandServiceImpl(repository, publishedEvents::add, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Long raise() {
        return service.handle(new RaiseSecurityAlertCommand(1L, 3L, 2L, AlertType.UNAUTHORIZED_ACCESS, null, "Access out of shift"))
                .success().orElseThrow().getId();
    }

    @Test
    void raiseCreatesOpenCriticalAlertAndPublishesEvent() {
        var id = raise();
        var alert = alerts.get(id);
        assertEquals(AlertStatus.OPEN, alert.getStatus());
        assertEquals(AlertSeverity.CRITICAL, alert.getSeverity());
        assertEquals(NOW, alert.getRaisedAt());
        assertEquals(1, publishedEvents.size());
    }

    @Test
    void falseAlarmClosesTheAlertAndPublishesEvent() {
        var id = raise();
        publishedEvents.clear();
        var result = service.handle(new MarkAlertAsFalseAlarmCommand(id, "Autorizado verbalmente", "lanahua"));
        assertEquals(AlertStatus.FALSE_ALARM, result.success().orElseThrow().getStatus());
        assertEquals(1, publishedEvents.size());
    }

    @Test
    void falseAlarmWithoutJustificationIsRejected() {
        var id = raise();
        assertThrows(IllegalArgumentException.class,
                () -> service.handle(new MarkAlertAsFalseAlarmCommand(id, null, "lanahua")));
        assertEquals(AlertStatus.OPEN, alerts.get(id).getStatus());
    }

    @Test
    void closingTwiceReturnsAlreadyResolved() {
        var id = raise();
        service.handle(new ReconcileAlertCommand(id, null, "lanahua"));
        var second = service.handle(new MarkAlertAsFalseAlarmCommand(id, "Otro intento de cierre", "lanahua"));
        assertInstanceOf(SecurityAlertCommandFailure.AlreadyResolved.class, second.failure().orElseThrow());
    }

    @Test
    void unknownAlertReturnsNotFound() {
        var result = service.handle(new ReconcileAlertCommand(99L, null, "lanahua"));
        assertInstanceOf(SecurityAlertCommandFailure.NotFound.class, result.failure().orElseThrow());
    }
}
