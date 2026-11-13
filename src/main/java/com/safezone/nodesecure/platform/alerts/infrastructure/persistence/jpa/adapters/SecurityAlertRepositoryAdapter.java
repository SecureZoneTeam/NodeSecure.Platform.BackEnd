package com.safezone.nodesecure.platform.alerts.infrastructure.persistence.jpa.adapters;

import com.safezone.nodesecure.platform.alerts.domain.model.aggregates.SecurityAlert;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertSeverity;
import com.safezone.nodesecure.platform.alerts.domain.model.valueobjects.AlertStatus;
import com.safezone.nodesecure.platform.alerts.domain.repositories.SecurityAlertRepository;
import com.safezone.nodesecure.platform.alerts.infrastructure.persistence.jpa.SecurityAlertJpaRepository;
import com.safezone.nodesecure.platform.alerts.infrastructure.persistence.jpa.entities.SecurityAlertJpaEntity;
import com.safezone.nodesecure.platform.alerts.infrastructure.persistence.jpa.mappers.SecurityAlertPersistenceMapper;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SecurityAlertRepositoryAdapter implements SecurityAlertRepository {
    private final SecurityAlertJpaRepository jpa;
    private final SecurityAlertPersistenceMapper mapper;

    public SecurityAlertRepositoryAdapter(SecurityAlertJpaRepository jpa, SecurityAlertPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public SecurityAlert save(SecurityAlert alert) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(alert)));
    }

    @Override
    public Optional<SecurityAlert> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SecurityAlert> findByIdForUpdate(Long id) {
        return jpa.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override
    public List<SecurityAlert> findAll(AlertStatus status, AlertSeverity severity, Long warehouseId) {
        Specification<SecurityAlertJpaEntity> spec = (root, query, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status.name()));
        }
        if (severity != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity.name()));
        }
        if (warehouseId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("warehouseId"), warehouseId));
        }
        var newestFirst = Sort.by(Sort.Order.desc("raisedAt"), Sort.Order.desc("id"));
        return jpa.findAll(spec, newestFirst).stream().map(mapper::toDomain).toList();
    }
}
