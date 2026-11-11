package com.safezone.nodesecure.platform.iam.interfaces.rest.transform;

import com.safezone.nodesecure.platform.iam.application.commands.CreateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignInCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignUpCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateUserCommand;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.CreateUserResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.SignInResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.SignUpResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.UpdateUserResource;

public class UserCommandMapper {

    public static SignInCommand toSignInCommand(SignInResource resource) {
        return new SignInCommand(resource.username(), resource.password());
    }

    public static SignUpCommand toSignUpCommand(SignUpResource resource) {
        return new SignUpCommand(resource.username(), resource.email(), resource.password(),
                resource.fullName(), resource.companyId());
    }

    public static CreateUserCommand toCreateCommand(CreateUserResource resource, Long currentCompanyId) {
        Long companyId = resource.companyId() != null ? resource.companyId() : currentCompanyId;
        return new CreateUserCommand(resource.username(), resource.email(), resource.password(),
                resource.fullName(), resource.roles(), companyId, resource.warehouseId());
    }

    public static UpdateUserCommand toUpdateCommand(Long id, UpdateUserResource resource) {
        return new UpdateUserCommand(id, resource.email(), resource.fullName(), resource.roles(), resource.warehouseId());
    }
}
