package com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa;

import com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.entities.OrganizationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationJpaRepository extends JpaRepository<OrganizationJpaEntity, Long> {
}
