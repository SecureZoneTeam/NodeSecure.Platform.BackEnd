package com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.repositories;

import com.safezone.nodesecure.platform.sensors.infrastructure.persistence.jpa.entities.PhysicalEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhysicalEventJpaRepository extends JpaRepository<PhysicalEventJpaEntity, Long> {
    List<PhysicalEventJpaEntity> findAllByCompanyIdOrderByOccurredAtDesc(Long companyId);
    boolean existsBySensorNodeId(Long sensorNodeId);
}
