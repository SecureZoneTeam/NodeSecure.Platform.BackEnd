package com.safezone.nodesecure.platform.warehouses.domain.model.aggregates;

import com.safezone.nodesecure.platform.shared.domain.aggregates.AbstractDomainAggregateRoot;
import com.safezone.nodesecure.platform.warehouses.domain.model.entities.WarehouseZone;
import com.safezone.nodesecure.platform.warehouses.domain.model.events.WarehouseRegisteredEvent;
import com.safezone.nodesecure.platform.warehouses.domain.model.events.WarehouseZoneConfiguredEvent;
import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.StreetAddress;
import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.WarehouseStatus;
import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.OperatingShift;
import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.ZoneRiskLevel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Aggregate root that owns the infrastructure zones of one warehouse. */
public final class Warehouse extends AbstractDomainAggregateRoot {

    private static final int MAX_NAME_LENGTH = 80;

    private final Long id;
    private final Long companyId;
    private String name;
    private StreetAddress address;
    private WarehouseStatus status;
    private final List<WarehouseZone> zones;
    private final Instant createdAt;
    private Instant updatedAt;

    private Warehouse(Long id, Long companyId, String name, StreetAddress address, WarehouseStatus status,
                      List<WarehouseZone> zones, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.companyId = requirePositive(companyId, "warehouse.error.company.required");
        this.name = validateName(name);
        this.address = Objects.requireNonNull(address, "warehouse.error.address.required");
        this.status = Objects.requireNonNull(status, "warehouse.error.status.invalid");
        this.zones = new ArrayList<>(zones == null ? List.of() : zones);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
    }

    public static Warehouse create(Long companyId, String name, StreetAddress address, Instant now) {
        Warehouse warehouse = new Warehouse(null, companyId, name, address, WarehouseStatus.ACTIVE,
                List.of(), now, now);
        warehouse.registerEvent(new WarehouseRegisteredEvent(companyId, warehouse.getName(), now));
        return warehouse;
    }

    public static Warehouse restore(Long id, Long companyId, String name, StreetAddress address,
                                    WarehouseStatus status, List<WarehouseZone> zones,
                                    Instant createdAt, Instant updatedAt) {
        if (id == null || id <= 0) throw new IllegalArgumentException("warehouse.error.id.invalid");
        return new Warehouse(id, companyId, name, address, status, zones, createdAt, updatedAt);
    }

    public void updateInformation(String name, StreetAddress address, WarehouseStatus status, Instant now) {
        this.name = validateName(name);
        this.address = Objects.requireNonNull(address, "warehouse.error.address.required");
        this.status = Objects.requireNonNull(status, "warehouse.error.status.invalid");
        this.updatedAt = Objects.requireNonNull(now);
    }

    public WarehouseZone addZone(String name, String code, ZoneRiskLevel riskLevel,
                                 OperatingShift operatingShift, Instant now) {
        if (id == null) throw new IllegalStateException("warehouse.error.zone.warehouse.notPersisted");
        if (hasZoneCode(code, null)) throw new IllegalArgumentException("warehouse.error.zone.codeTaken");
        WarehouseZone zone = WarehouseZone.create(id, name, code, riskLevel, operatingShift);
        zones.add(zone);
        updatedAt = Objects.requireNonNull(now);
        registerEvent(new WarehouseZoneConfiguredEvent(id, null, zone.getCode(), now));
        return zone;
    }

    public WarehouseZone updateZone(Long zoneId, String name, String code, ZoneRiskLevel riskLevel,
                                    OperatingShift shift, Instant now) {
        WarehouseZone zone = findZone(zoneId);
        if (zone == null) throw new IllegalArgumentException("warehouse.error.zone.notFound");
        if (hasZoneCode(code, zoneId)) throw new IllegalArgumentException("warehouse.error.zone.codeTaken");
        zone.updateInformation(name, code, riskLevel, shift);
        updatedAt = Objects.requireNonNull(now);
        registerEvent(new WarehouseZoneConfiguredEvent(id, zoneId, zone.getCode(), now));
        return zone;
    }

    public boolean removeZone(Long zoneId, Instant now) {
        boolean removed = zones.removeIf(zone -> Objects.equals(zone.getId(), zoneId));
        if (removed) updatedAt = Objects.requireNonNull(now);
        return removed;
    }

    public WarehouseZone findZone(Long zoneId) {
        return zones.stream().filter(zone -> Objects.equals(zone.getId(), zoneId)).findFirst().orElse(null);
    }

    public boolean hasZoneCode(String code, Long excludedZoneId) {
        if (code == null) return false;
        return zones.stream().anyMatch(zone -> zone.getCode().equalsIgnoreCase(code.trim())
                && !Objects.equals(zone.getId(), excludedZoneId));
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("warehouse.error.name.required");
        String clean = name.trim();
        if (clean.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("warehouse.error.name.tooLong");
        return clean;
    }

    private static Long requirePositive(Long value, String key) {
        if (value == null || value <= 0) throw new IllegalArgumentException(key);
        return value;
    }

    public Long getId() { return id; }
    public Long getCompanyId() { return companyId; }
    public String getName() { return name; }
    public StreetAddress getAddress() { return address; }
    public WarehouseStatus getStatus() { return status; }
    public List<WarehouseZone> getZones() { return List.copyOf(zones); }
    public int getZoneCount() { return zones.size(); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
