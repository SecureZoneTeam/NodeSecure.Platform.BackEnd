package com.safezone.nodesecure.platform.iam.interfaces.rest.transform;

import com.safezone.nodesecure.platform.iam.application.commands.CreateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignInCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignUpCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateUserCommand;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.CreateUserResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.SignInResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.SignUpResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.UpdateUserResource;

import java.util.List;

/** Organization and actor ids always come from the session and are passed in by the controller. */
public final class UserCommandMapper {

    private UserCommandMapper() {
    }

    public static SignInCommand toSignInCommand(SignInResource resource) {
        return new SignInCommand(resource.username(), resource.password());
    }

    public static SignUpCommand toSignUpCommand(SignUpResource resource) {
        return new SignUpCommand(resource.username(), resource.email(), resource.password(),
                resource.fullName(), resource.organizationName());
    }

    public static CreateUserCommand toCreateCommand(CreateUserResource resource, Long companyId, Long actorUserId) {
        List<Long> warehouseIds = resource.warehouseIds() != null && !resource.warehouseIds().isEmpty()
                ? resource.warehouseIds()
                : (resource.warehouseId() == null ? List.of() : List.of(resource.warehouseId()));
        return new CreateUserCommand(companyId, actorUserId, resource.username(), resource.email(),
                resource.password(), resource.fullName(), resource.role(), warehouseIds);
    }

    public static UpdateUserCommand toUpdateCommand(Long id, UpdateUserResource resource, Long companyId,
                                                    Long actorUserId) {
        return new UpdateUserCommand(companyId, actorUserId, id, resource.email(), resource.fullName(),
                resource.role(), resource.warehouseIds());
    }
}
