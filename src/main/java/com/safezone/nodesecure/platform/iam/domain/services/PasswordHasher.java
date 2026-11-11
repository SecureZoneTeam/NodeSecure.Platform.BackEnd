package com.safezone.nodesecure.platform.iam.domain.services;

public interface PasswordHasher {

    String hash(String password);

    boolean matches(String password, String hash);
}
