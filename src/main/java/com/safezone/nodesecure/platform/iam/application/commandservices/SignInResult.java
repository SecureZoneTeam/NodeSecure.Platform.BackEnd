package com.safezone.nodesecure.platform.iam.application.commandservices;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;

public record SignInResult(User user, String token) {
}
