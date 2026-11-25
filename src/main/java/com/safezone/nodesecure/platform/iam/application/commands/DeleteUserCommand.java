package com.safezone.nodesecure.platform.iam.application.commands;

public record DeleteUserCommand(Long companyId, Long actorUserId, Long id) {
}
