package com.safezone.nodesecure.platform.iam.application.commands;

/** The signed-in user replaces its password; the current one must be confirmed. */
public record ChangeOwnPasswordCommand(Long userId, Long companyId, String currentPassword, String newPassword) {

    @Override
    public String toString() {
        return "ChangeOwnPasswordCommand[userId=" + userId + ", companyId=" + companyId + "]";
    }
}
