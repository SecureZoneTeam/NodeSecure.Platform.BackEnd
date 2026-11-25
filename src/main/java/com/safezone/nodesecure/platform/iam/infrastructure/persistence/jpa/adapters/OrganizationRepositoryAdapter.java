package com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.adapters;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.Organization;
import com.safezone.nodesecure.platform.iam.domain.repositories.OrganizationRepository;
import com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.OrganizationJpaRepository;
import com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.entities.OrganizationJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrganizationRepositoryAdapter implements OrganizationRepository {

    private final OrganizationJpaRepository jpa;

    public OrganizationRepositoryAdapter(OrganizationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Organization save(Organization organization) {
        OrganizationJpaEntity saved = jpa.saveAndFlush(new OrganizationJpaEntity(
                organization.getId(),
                organization.getName(),
                organization.getOwnerUserId(),
                organization.getStatus().name(),
                organization.getCreatedAt(),
                organization.getUpdatedAt()));
        return toDomain(saved);
    }

    @Override
    public Optional<Organization> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return jpa.findById(id).map(OrganizationRepositoryAdapter::toDomain);
    }

    private static Organization toDomain(OrganizationJpaEntity entity) {
        return Organization.restore(entity.getId(), entity.getName(), entity.getOwnerUserId(),
                Organization.Status.valueOf(entity.getStatus()), entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
