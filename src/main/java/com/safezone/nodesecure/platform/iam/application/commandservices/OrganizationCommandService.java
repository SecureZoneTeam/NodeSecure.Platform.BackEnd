package com.safezone.nodesecure.platform.iam.application.commandservices;

import com.safezone.nodesecure.platform.iam.application.commands.UpdateOrganizationCommand;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.Organization;

import java.util.Optional;

public interface OrganizationCommandService {

    /** Empty when the organization does not exist. */
    Optional<Organization> handle(UpdateOrganizationCommand command);
}
