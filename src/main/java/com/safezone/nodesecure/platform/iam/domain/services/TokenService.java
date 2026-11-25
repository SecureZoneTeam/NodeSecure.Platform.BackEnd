package com.safezone.nodesecure.platform.iam.domain.services;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.TokenClaims;

import java.util.Optional;

public interface TokenService {

    String createToken(User user);

    Optional<TokenClaims> readToken(String token);
}
