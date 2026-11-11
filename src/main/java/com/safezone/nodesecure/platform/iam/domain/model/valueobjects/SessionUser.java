package com.safezone.nodesecure.platform.iam.domain.model.valueobjects;

import java.util.List;

public record SessionUser(Long id, String username, Long companyId, Long warehouseId, List<String> roles) {

    public SessionUser {
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
