package com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.mappers;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Permission;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.UserStatus;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.entities.UserJpaEntity;
import com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.entities.UserPermissionOverrideEmbeddable;
import com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.entities.UserWarehouseAssignmentEmbeddable;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserPersistenceMapper {

    public UserJpaEntity toEntity(User user) {
        Long companyId = user.getCompanyId();
        Set<UserWarehouseAssignmentEmbeddable> warehouses = user.getWarehouseIds().stream()
                .map(warehouseId -> new UserWarehouseAssignmentEmbeddable(warehouseId, companyId))
                .collect(Collectors.toSet());
        Set<UserPermissionOverrideEmbeddable> permissions = user.getPermissionOverrides().entrySet().stream()
                .map(entry -> new UserPermissionOverrideEmbeddable(entry.getKey().code(), entry.getValue(), companyId))
                .collect(Collectors.toSet());
        return new UserJpaEntity(
                user.getId(),
                user.getUsername().value(),
                user.getEmail().value(),
                user.getFullName(),
                user.getPasswordHash(),
                user.getRole().name(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getTokenVersion(),
                companyId,
                user.primaryWarehouseId(),
                warehouses,
                permissions,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public User toDomain(UserJpaEntity entity) {
        Role role = entity.getRole() == null || entity.getRole().isBlank()
                ? Role.fromLegacyRoles(entity.getLegacyRoles())
                : Role.from(entity.getRole());
        Set<Long> warehouseIds = entity.getWarehouses().stream()
                .filter(assignment -> assignment.getCompanyId() != null
                        && assignment.getCompanyId().equals(entity.getCompanyId()))
                .map(UserWarehouseAssignmentEmbeddable::getWarehouseId)
                .collect(Collectors.toSet());
        Map<Permission, Boolean> overrides = new EnumMap<>(Permission.class);
        entity.getPermissions().forEach(row -> Permission.fromCode(row.getPermissionCode())
                .ifPresent(permission -> overrides.put(permission, row.isGranted())));
        return User.restore(
                entity.getId(),
                new Username(entity.getUsername()),
                new EmailAddress(entity.getEmail()),
                entity.getFullName(),
                entity.getPasswordHash(),
                role,
                UserStatus.from(entity.getStatus()),
                entity.getTokenVersion(),
                entity.getCompanyId(),
                warehouseIds,
                overrides,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
