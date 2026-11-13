# Warehouse Management & Sensor Integration — Implementation Notes

Owner: **Kelber Sandoval**  
Scope: **US01, US02, US04, US05, US19, TS09**

## Architecture applied

The two bounded contexts follow the backend's existing DDD layering and the professor's reference patterns:

- **Domain:** pure aggregates, child entities, value objects, repository ports and domain events.
- **Application:** immutable command/query records, command/query service contracts and implementations, and `Result<T, Failure>` for expected business outcomes.
- **Infrastructure:** Spring Data JPA repositories, persistence entities, mappers and repository adapters. JPA annotations stay outside the domain model.
- **Interfaces:** REST request/response resources, command/resource mappers, controllers and ACL contracts.

`Warehouse` is the aggregate root that owns its `WarehouseZone` entities. The zone collection is exposed as a defensive copy. Zone-specific REST routes do not make `WarehouseZone` an independent aggregate.

`SensorNode` and `PhysicalEvent` are separate aggregate roots. A sensor stores the zone ID and company ID, while the cross-context relationship is checked through `WarehouseContextFacade`.

## REST contract

All management routes use the authenticated user's `companyId`; the client cannot select another company by sending a company ID in JSON.

| Method | Route | Purpose |
|---|---|---|
| GET/POST | `/api/v1/warehouses` | List and register warehouses |
| GET/PUT/DELETE | `/api/v1/warehouses/{id}` | Retrieve, update or delete an empty warehouse |
| GET/POST | `/api/v1/warehouse-zones` | List and create warehouse zones |
| GET/PUT/DELETE | `/api/v1/warehouse-zones/{id}` | Retrieve, update or delete a zone |
| GET/POST | `/api/v1/sensor-nodes` | List and register IoT sensor nodes |
| GET/PUT/DELETE | `/api/v1/sensor-nodes/{id}` | Retrieve, update or delete a sensor without historical events |
| POST | `/api/v1/physical-events` | Receive ESP32 physical events (TS09) |

## Business rules

1. Warehouse names are unique inside a company; zone codes are unique inside a warehouse.
2. A warehouse cannot be deleted while it contains zones.
3. A zone cannot be deleted while one or more sensor nodes reference it.
4. A sensor MAC address is normalized to uppercase and is globally unique. Once physical events exist, the MAC address cannot be changed, preserving historical device identity.
5. A sensor can only be linked to a zone belonging to the authenticated company.
6. New sensors start `OFFLINE`. `DOOR_OPENED` and `DOOR_CLOSED` events refresh the last ping and set the sensor to `ONLINE`; `NODE_DISCONNECTED` marks it `OFFLINE`.
7. When a sensor's last ping is older than `SENSOR_CONNECTION_TIMEOUT_SECONDS` (default 120, provisional until the hardware heartbeat interval is agreed), the next sensor query marks it `OFFLINE`. This check is query-driven; it is not a background scheduler.
8. Physical events are immutable in this context and are initially stored with `reconciled=false`. Alert reconciliation belongs to the Alerts/Discrepancy context.
9. Zone opening and closing times use strict `HH:mm` format. The supplied acceptance criteria do not define whether overnight intervals are allowed, so the domain preserves both valid times without imposing an ordering rule. The later Alerts context must interpret the interval together with the event date/time.

## Cross-context collaboration

- `WarehouseContextFacade` is the public ACL contract used by Sensor Integration. It validates zone/company ownership and exposes warehouse status, zone risk and operating-shift configuration.
- `SensorContextFacade` is the public ACL contract used by Warehouse Management. It prevents deletion of a zone that is still referenced by a registered sensor.
- `IamContextFacade` scopes reads for `ROLE_WAREHOUSE_KEEPER` to `currentWarehouseId()`. The current `SessionUser` does not expose assigned zone IDs, so exact per-zone authorization cannot be implemented inside these contexts without a coordinated IAM change; the current enforceable scope is the assigned warehouse.
- `PhysicalEventReceivedEvent` includes company, sensor, zone, event type and event time so Alerts/Reporting can subscribe without depending on Sensor Integration internals.

## Device event authentication

`POST /api/v1/physical-events` is outside JWT user authentication and is protected by `DeviceApiKeyFilter`. Each request must include `X-Device-Api-Key`, matching the `SENSOR_API_KEY` environment variable. The filter fails closed if the variable is missing. The API key must be kept on the IoT device/integration side and must never be embedded in the Angular application.

Accepted JSON fields are `macAddress`, `eventType`, `occurredAt`; aliases `mac`, `state`, `timestamp` support the ESP32 payload format. The client cannot submit `sensorNodeId`, `zoneId` or `companyId`: those values are derived from the registered MAC address.

## Validation

Domain unit tests cover warehouse creation and zones, operating-shift validation, MAC normalization, sensor connection transitions and immutable physical-event creation. Run the full project suite with:

```bash
./mvnw clean test
```

The Maven wrapper must be able to download Maven 3.9.11 and resolve the project dependencies before this command can run.
