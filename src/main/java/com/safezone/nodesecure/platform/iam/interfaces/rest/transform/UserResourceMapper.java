package com.safezone.nodesecure.platform.iam.interfaces.rest.transform;

import com.safezone.nodesecure.platform.iam.application.commandservices.SignInResult;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.Organization;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Permission;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.services.RolePermissions;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.AuthenticatedUserResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.OrganizationResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.PermissionResource;
import com.safezone.nodesecure.platform.iam.interfaces.rest.resources.UserResource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class UserResourceMapper {

    private UserResourceMapper() {
    }

    public static UserResource toResource(User user) {
        Map<String, Boolean> overrides = new LinkedHashMap<>();
        user.getPermissionOverrides().forEach((permission, granted) -> overrides.put(permission.code(), granted));
        return new UserResource(
                user.getId(),
                user.getUsername().value(),
                user.getEmail().value(),
                user.getFullName(),
                user.getRole().name(),
                List.of(user.getRole().name()),
                user.getStatus().name(),
                user.getCompanyId(),
                List.copyOf(user.getWarehouseIds()),
                user.primaryWarehouseId(),
                overrides,
                permissionCodes(user),
                user.getCreatedAt());
    }

    public static AuthenticatedUserResource toAuthenticatedResource(SignInResult result) {
        return toAuthenticatedResource(result.user(), result.organization(), result.token());
    }

    public static AuthenticatedUserResource toAuthenticatedResource(User user, Organization organization, String token) {
        return new AuthenticatedUserResource(
                user.getId(),
                user.getUsername().value(),
                user.getFullName(),
                user.getEmail().value(),
                token,
                user.getRole().name(),
                List.of(user.getRole().name()),
                permissionCodes(user),
                user.isOwner(),
                List.copyOf(user.getWarehouseIds()),
                user.primaryWarehouseId(),
                organization == null ? user.getCompanyId() : organization.getId(),
                organization == null ? null : organization.getName());
    }

    public static OrganizationResource toResource(Organization organization) {
        return new OrganizationResource(organization.getId(), organization.getName(), organization.getOwnerUserId(),
                organization.getStatus().name(), organization.getCreatedAt());
    }

    public static List<PermissionResource> toPermissionCatalog() {
        return Arrays.stream(Permission.values())
                .map(permission -> new PermissionResource(
                        permission.code(),
                        permission.ownerOnly(),
                        Arrays.stream(Role.values())
                                .filter(role -> RolePermissions.defaultsFor(role).contains(permission))
                                .map(Role::name)
                                .toList()))
                .toList();
    }

    private static List<String> permissionCodes(User user) {
        return user.effectivePermissions().stream().map(Permission::code).sorted().toList();
    }
}
