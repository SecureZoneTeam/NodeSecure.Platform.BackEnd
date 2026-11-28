package com.safezone.nodesecure.platform.reporting.application.internal.eventhandlers;

import com.safezone.nodesecure.platform.alerts.domain.model.events.SecurityAlertRaisedEvent;
import com.safezone.nodesecure.platform.alerts.domain.model.events.SecurityAlertResolvedEvent;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertType;
import com.safezone.nodesecure.platform.iam.domain.model.events.UserRegisteredEvent;
import com.safezone.nodesecure.platform.inventory.domain.model.events.ProductCreatedEvent;
import com.safezone.nodesecure.platform.inventory.domain.model.events.StockMovementRegisteredEvent;
import com.safezone.nodesecure.platform.inventory.domain.model.valueobjects.StockMovementType;
import com.safezone.nodesecure.platform.reporting.application.internal.services.TraceabilityRecorder;
import com.safezone.nodesecure.platform.reporting.application.internal.services.TraceabilityRecorder.Event;
import com.safezone.nodesecure.platform.reporting.domain.model.valueobjects.TraceabilityRecordType;
import com.safezone.nodesecure.platform.sensors.domain.model.events.PhysicalEventReceivedEvent;
import com.safezone.nodesecure.platform.sensors.domain.model.events.SensorNodeRegisteredEvent;
import com.safezone.nodesecure.platform.sensors.domain.model.valueobjects.PhysicalEventType;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * T29: translates the domain events of users, sensors, inventory and alerts into entries of the
 * traceability log. Listeners run synchronously inside the publisher's transaction.
 */
@Component
public class TraceabilityEventHandler {

    private final TraceabilityRecorder recorder;

    public TraceabilityEventHandler(TraceabilityRecorder recorder) {
        this.recorder = recorder;
    }

    @EventListener
    public void on(UserRegisteredEvent event) {
        recorder.record(new Event(event.companyId(), null, null, TraceabilityRecordType.USER_MANAGEMENT,
                "USER_REGISTERED", "USER", event.username(), event.username(),
                "User " + event.username() + " registered in the organization.", event.occurredAt()));
    }

    @EventListener
    public void on(SensorNodeRegisteredEvent event) {
        recorder.record(new Event(event.companyId(), null, event.zoneId(), TraceabilityRecordType.SENSOR_MANAGEMENT,
                "SENSOR_NODE_REGISTERED", "SENSOR_NODE", event.macAddress(), null,
                "Sensor node " + event.macAddress() + " (" + event.sensorType() + ") registered in zone "
                        + event.zoneId() + ".", event.occurredAt()));
    }

    @EventListener
    public void on(PhysicalEventReceivedEvent event) {
        boolean disconnection = event.eventType() == PhysicalEventType.NODE_DISCONNECTED;
        String description = switch (event.eventType()) {
            case DOOR_OPENED -> "Door opened: physical access detected by sensor node " + event.sensorNodeId() + ".";
            case DOOR_CLOSED -> "Door closed: reported by sensor node " + event.sensorNodeId() + ".";
            case NODE_DISCONNECTED -> "Sensor node " + event.sensorNodeId() + " disconnected.";
        };
        recorder.record(new Event(event.companyId(), null, event.zoneId(),
                disconnection ? TraceabilityRecordType.NODE_DISCONNECTION : TraceabilityRecordType.PHYSICAL_ACCESS,
                "PHYSICAL_EVENT_" + event.eventType().name(), "SENSOR_NODE", String.valueOf(event.sensorNodeId()),
                null, description, event.occurredAt()));
    }

    @EventListener
    public void on(ProductCreatedEvent event) {
        recorder.record(new Event(event.companyId(), null, null, TraceabilityRecordType.PRODUCT_CATALOG,
                "PRODUCT_CREATED", "PRODUCT", event.sku(), null,
                "Product " + event.sku() + " added to the catalog.", event.occurredAt()));
    }

    @EventListener
    public void on(StockMovementRegisteredEvent event) {
        boolean reconciliation = event.type() == StockMovementType.RECONCILIATION;
        recorder.record(new Event(event.companyId(), event.warehouseId(), null,
                reconciliation ? TraceabilityRecordType.DISCREPANCY : TraceabilityRecordType.INVENTORY_MOVEMENT,
                "STOCK_" + event.type().name(), "PRODUCT", String.valueOf(event.productId()), null,
                event.type().name() + " of " + event.quantity() + " unit(s) of product " + event.productId()
                        + "; resulting stock " + event.resultingStock() + ".", event.occurredAt()));
    }

    @EventListener
    public void on(SecurityAlertRaisedEvent event) {
        TraceabilityRecordType type = event.type() == AlertType.DISCREPANCY ? TraceabilityRecordType.DISCREPANCY
                : event.type() == AlertType.NODE_DISCONNECTION ? TraceabilityRecordType.NODE_DISCONNECTION
                : TraceabilityRecordType.SECURITY_ALERT;
        recorder.record(new Event(null, event.warehouseId(), event.zoneId(), type, "SECURITY_ALERT_RAISED",
                "PHYSICAL_EVENT", event.physicalEventId() == null ? null : String.valueOf(event.physicalEventId()), null,
                event.severity() + " " + event.type() + " alert raised.", event.occurredAt()));
    }

    @EventListener
    public void on(SecurityAlertResolvedEvent event) {
        recorder.record(new Event(null, event.warehouseId(), event.zoneId(), TraceabilityRecordType.SECURITY_ALERT,
                "SECURITY_ALERT_" + event.status().name(), "SECURITY_ALERT", String.valueOf(event.alertId()),
                event.resolvedBy(), "Alert " + event.alertId() + " closed as " + event.status() + ".",
                event.occurredAt()));
    }
}
