package com.safezone.nodesecure.platform.iam.application.internal.commandservices;

import com.safezone.nodesecure.platform.iam.application.commands.UpdateOrganizationCommand;
import com.safezone.nodesecure.platform.iam.application.commandservices.OrganizationCommandService;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.Organization;
import com.safezone.nodesecure.platform.iam.domain.repositories.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

@Service
public class OrganizationCommandServiceImpl implements OrganizationCommandService {

    private final OrganizationRepository repository;
    private final Clock clock;

    public OrganizationCommandServiceImpl(OrganizationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Optional<Organization> handle(UpdateOrganizationCommand command) {
        return repository.findById(command.companyId()).map(organization -> {
            organization.rename(command.name(), clock.instant());
            return repository.save(organization);
        });
    }
}
