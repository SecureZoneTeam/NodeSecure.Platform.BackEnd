package com.safezone.nodesecure.platform.iam.application.commands;

/** The signed-in user edits its own profile. Null or blank fields keep their current value. */
public record UpdateOwnProfileCommand(Long userId, Long companyId, String fullName, String email, String username) {
}
