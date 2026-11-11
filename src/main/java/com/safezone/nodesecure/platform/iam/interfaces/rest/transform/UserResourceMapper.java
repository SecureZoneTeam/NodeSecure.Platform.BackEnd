package com.safezone.nodesecure.platform.iam.interfaces.rest.transform;

import com.safezone.nodesecure.platform.iam.application.commandservices.SignInResult;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.UserResource;

public class UserResourceMapper {

    public static UserResource toResource(User user) {
        return new UserResource(
                user.getId(),
                user.getUsername().value(),
                user.getEmail().value(),
                user.getFullName(),
                user.getRoleNames(),
                user.getCompanyId(),
                user.getWarehouseId());
    }

    public static AuthenticatedUserResource toAuthenticatedResource(SignInResult result) {
        User user = result.user();
        return new AuthenticatedUserResource(
                user.getId(),
                user.getUsername().value(),
                result.token(),
                user.getRoleNames(),
                user.getWarehouseId());
    }
}
