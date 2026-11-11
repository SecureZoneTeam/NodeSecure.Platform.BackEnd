package com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.mappers;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.entities.UserJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

@Component
public class UserPersistenceMapper {

    public UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.getId(),
                user.getUsername().value(),
                user.getEmail().value(),
                user.getFullName(),
                user.getPasswordHash(),
                String.join(",", user.getRoleNames()),
                user.getCompanyId(),
                user.getWarehouseId(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    public User toDomain(UserJpaEntity entity) {
        return User.restore(
                entity.getId(),
                new Username(entity.getUsername()),
                new EmailAddress(entity.getEmail()),
                entity.getFullName(),
                entity.getPasswordHash(),
                toRoles(entity.getRoles()),
                entity.getCompanyId(),
                entity.getWarehouseId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private Set<Role> toRoles(String text) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        Arrays.stream(text.split(","))
                .filter(name -> !name.isBlank())
                .map(Role::from)
                .forEach(roles::add);
        return roles;
    }
}
