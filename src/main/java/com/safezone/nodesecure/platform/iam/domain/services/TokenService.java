package com.safezone.nodesecure.platform.iam.domain.services;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.SessionUser;

import java.util.Optional;

public interface TokenService {

    String createToken(User user);

    Optional<SessionUser> readToken(String token);
}
