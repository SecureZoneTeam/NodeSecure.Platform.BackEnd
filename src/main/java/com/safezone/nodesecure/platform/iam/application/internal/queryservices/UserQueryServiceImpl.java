package com.safezone.nodesecure.platform.iam.application.internal.queryservices;

import com.safezone.nodesecure.platform.iam.application.queries.GetAllUsersQuery;
import com.safezone.nodesecure.platform.iam.application.queries.GetOrganizationByIdQuery;
import com.safezone.nodesecure.platform.iam.application.queries.GetUserByIdQuery;
import com.safezone.nodesecure.platform.iam.application.queryservices.UserQueryService;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.Organization;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.repositories.OrganizationRepository;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository repository;
    private final OrganizationRepository organizationRepository;

    public UserQueryServiceImpl(UserRepository repository, OrganizationRepository organizationRepository) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
    }

    @Override
    public List<User> handle(GetAllUsersQuery query) {
        return repository.findAllByCompanyId(query.companyId());
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        return repository.findByIdAndCompanyId(query.id(), query.companyId());
    }

    @Override
    public Optional<Organization> handle(GetOrganizationByIdQuery query) {
        return organizationRepository.findById(query.id());
    }
}
