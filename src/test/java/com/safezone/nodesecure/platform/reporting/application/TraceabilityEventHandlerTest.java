package com.safezone.nodesecure.platform.reporting.application;

import com.safezone.nodesecure.platform.alerts.domain.model.events.SecurityAlertRaisedEvent;
import com.safezone.nodesecure.platform.alerts.domain.model.events.SecurityAlertResolvedEvent;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertSeverity;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertStatus;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertType;
import com.safezone.nodesecure.platform.iam.domain.model.events.UserRegisteredEvent;
import com.safezone.nodesecure.platform.inventory.domain.model.events.ProductCreatedEvent;
import com.safezone.nodesecure.platform.inventory.domain.model.events.StockMovementRegisteredEvent;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;
import com.safezone.nodesecure.platform.reporting.application.internal.eventhandlers.TraceabilityEventHandler;
import com.safezone.nodesecure.platform.reporting.application.internal.services.TraceabilityRecorder;
import com.safezone.nodesecure.platform.reporting.application.internal.services.TraceabilityRecorder.Event;
import com.safezone.nodesecure.platform.reporting.domain.model.valueobjects.TraceabilityRecordType;
import com.safezone.nodesecure.platform.sensors.domain.model.events.PhysicalEventReceivedEvent;
import com.safezone.nodesecure.platform.sensors.domain.model.events.SensorNodeRegisteredEvent;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.PhysicalEventType;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.SensorType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** T29: every domain event of users, sensors, inventory and alerts becomes a traceability entry. */
class TraceabilityEventHandlerTest {

    private static final Instant NOW = Instant.parse("2026-11-20T08:00:00Z");

    private final TraceabilityRecorder recorder = mock(TraceabilityRecorder.class);
    private final TraceabilityEventHandler handler = new TraceabilityEventHandler(recorder);

    private Event captured() {
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(recorder).record(captor.capture());
        return captor.getValue();
    }

    @Test
    void userRegistration() {
        handler.on(new UserRegisteredEvent("pepito", 4L, NOW));
        Event event = captured();
        assertThat(event.recordType()).isEqualTo(TraceabilityRecordType.USER_MANAGEMENT);
        assertThat(event.companyId()).isEqualTo(4L);
        assertThat(event.entityId()).isEqualTo("pepito");
        assertThat(event.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void sensorRegistrationAndPhysicalEvents() {
        handler.on(new SensorNodeRegisteredEvent(4L, 9L, "AA:BB:CC:DD:EE:FF", SensorType.MAGNETIC_CONTACT, NOW));
        Event sensor = captured();
        assertThat(sensor.recordType()).isEqualTo(TraceabilityRecordType.SENSOR_MANAGEMENT);
        assertThat(sensor.zoneId()).isEqualTo(9L);

        TraceabilityRecorder recorder2 = mock(TraceabilityRecorder.class);
        TraceabilityEventHandler handler2 = new TraceabilityEventHandler(recorder2);
        handler2.on(new PhysicalEventReceivedEvent(4L, 3L, 9L, PhysicalEventType.NODE_DISCONNECTED, NOW));
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(recorder2).record(captor.capture());
        assertThat(captor.getValue().recordType()).isEqualTo(TraceabilityRecordType.NODE_DISCONNECTION);
        assertThat(captor.getValue().eventType()).isEqualTo("PHYSICAL_EVENT_NODE_DISCONNECTED");
    }

    @Test
    void doorOpeningIsPhysicalAccess() {
        handler.on(new PhysicalEventReceivedEvent(4L, 3L, 9L, PhysicalEventType.DOOR_OPENED, NOW));
        assertThat(captured().recordType()).isEqualTo(TraceabilityRecordType.PHYSICAL_ACCESS);
    }

    @Test
    void productCreation() {
        handler.on(new ProductCreatedEvent(4L, "NET-UTP6-305", NOW));
        Event event = captured();
        assertThat(event.recordType()).isEqualTo(TraceabilityRecordType.PRODUCT_CATALOG);
        assertThat(event.entityId()).isEqualTo("NET-UTP6-305");
    }

    @Test
    void reconciliationIsADiscrepancy() {
        handler.on(new StockMovementRegisteredEvent(4L, 8L, 2L, StockMovementType.RECONCILIATION, 3, 7, NOW));
        Event event = captured();
        assertThat(event.recordType()).isEqualTo(TraceabilityRecordType.DISCREPANCY);
        assertThat(event.warehouseId()).isEqualTo(2L);
        assertThat(event.eventType()).isEqualTo("STOCK_RECONCILIATION");
    }

    @Test
    void entryIsAnInventoryMovement() {
        handler.on(new StockMovementRegisteredEvent(4L, 8L, 2L, StockMovementType.ENTRY, 3, 7, NOW));
        assertThat(captured().recordType()).isEqualTo(TraceabilityRecordType.INVENTORY_MOVEMENT);
    }

    @Test
    void alertsRaisedAndResolved() {
        handler.on(new SecurityAlertRaisedEvent(2L, 9L, null, AlertType.UNAUTHORIZED_ACCESS, AlertSeverity.CRITICAL, NOW));
        Event raised = captured();
        assertThat(raised.recordType()).isEqualTo(TraceabilityRecordType.SECURITY_ALERT);
        assertThat(raised.warehouseId()).isEqualTo(2L);

        TraceabilityRecorder recorder2 = mock(TraceabilityRecorder.class);
        new TraceabilityEventHandler(recorder2).on(new SecurityAlertResolvedEvent(5L, 2L, 9L, null,
                AlertStatus.FALSE_ALARM, "Guard", NOW));
        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(recorder2).record(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("SECURITY_ALERT_FALSE_ALARM");
        assertThat(captor.getValue().entityId()).isEqualTo("5");
        assertThat(captor.getValue().actorFallback()).isEqualTo("Guard");
    }
}
