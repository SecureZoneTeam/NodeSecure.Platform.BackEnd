package com.safezone.nodesecure.platform.warehouses.infrastructure.persistence.jpa.entities;

import com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.ZoneRiskLevel;
import jakarta.persistence.*;

@Entity
@Table(name = "warehouse_zones", uniqueConstraints = @UniqueConstraint(name = "uk_warehouse_zone_code", columnNames = {"warehouse_id", "code"}))
public class WarehouseZoneJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private WarehouseJpaEntity warehouse;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private ZoneRiskLevel riskLevel;

    @Column(name = "opening_shift_start", nullable = false, length = 5)
    private String openingShiftStart;

    @Column(name = "opening_shift_end", nullable = false, length = 5)
    private String openingShiftEnd;

    public WarehouseZoneJpaEntity() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public WarehouseJpaEntity getWarehouse() { return warehouse; }
    public void setWarehouse(WarehouseJpaEntity warehouse) { this.warehouse = warehouse; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public ZoneRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(ZoneRiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public String getOpeningShiftStart() { return openingShiftStart; }
    public void setOpeningShiftStart(String openingShiftStart) { this.openingShiftStart = openingShiftStart; }
    public String getOpeningShiftEnd() { return openingShiftEnd; }
    public void setOpeningShiftEnd(String openingShiftEnd) { this.openingShiftEnd = openingShiftEnd; }
}
