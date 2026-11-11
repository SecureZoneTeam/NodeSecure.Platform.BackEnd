package com.safezone.nodesecure.platform.iam.application.commands;

import java.util.List;

public record CreateUserCommand(String username, String email, String password, String fullName,
                                List<String> roles, Long companyId, Long warehouseId) {
}
