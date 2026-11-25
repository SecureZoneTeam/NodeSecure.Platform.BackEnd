package com.safezone.nodesecure.platform.iam.domain.repositories;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.Organization;

import java.util.Optional;

public interface OrganizationRepository {
    Organization save(Organization organization);

    Optional<Organization> findById(Long id);
}
