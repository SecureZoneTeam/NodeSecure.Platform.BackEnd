package com.safezone.nodesecure.platform.iam.application.commands;

import java.util.List;

public record UpdateUserCommand(Long id, String email, String fullName, List<String> roles, Long warehouseId) {
}
