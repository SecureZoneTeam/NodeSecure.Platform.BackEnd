package com.safezone.nodesecure.platform.warehouses.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "warehouses", uniqueConstraints = @UniqueConstraint(name = "uk_warehouse_company_name", columnNames = {"company_id", "name"}))
public class WarehouseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 160)
    private String street;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 2)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.WarehouseStatus status;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<WarehouseZoneJpaEntity> zones = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public WarehouseJpaEntity() {
    }

    public void replaceZones(List<WarehouseZoneJpaEntity> newZones) {
        this.zones.clear();
        if (newZones != null) {
            for (WarehouseZoneJpaEntity zone : newZones) {
                zone.setWarehouse(this);
                this.zones.add(zone);
            }
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.WarehouseStatus getStatus() { return status; }
    public void setStatus(com.safezone.nodesecure.platform.warehouses.domain.model.valueobjects.WarehouseStatus status) { this.status = status; }
    public List<WarehouseZoneJpaEntity> getZones() { return zones; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
