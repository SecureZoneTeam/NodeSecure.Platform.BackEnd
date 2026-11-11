package com.safezone.nodesecure.platform.iam.application.commands;

public record SignUpCommand(String username, String email, String password, String fullName, Long companyId) {
}
