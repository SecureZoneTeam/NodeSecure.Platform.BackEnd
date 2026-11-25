package com.safezone.nodesecure.platform.iam.application.internal.sessions;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.SessionUser;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.TokenClaims;
import com.safezone.nodesecure.platform.iam.domain.repositories.OrganizationRepository;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Rebuilds the authenticated principal from the database on every request.
 * A token stops working as soon as the user is suspended or deleted, the organization is
 * suspended, or the token version changes. Role, permissions and warehouses always come
 * from the current database state, never from the token.
 */
@Service
public class SessionUserResolver {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    public SessionUserResolver(UserRepository userRepository, OrganizationRepository organizationRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public Optional<SessionUser> resolve(TokenClaims claims) {
        if (claims == null || claims.userId() == null || claims.companyId() == null) {
            return Optional.empty();
        }
        return userRepository.findByIdAndCompanyId(claims.userId(), claims.companyId())
                .filter(User::isActive)
                .filter(user -> user.getTokenVersion() == claims.tokenVersion())
                .filter(user -> organizationRepository.findById(user.getCompanyId())
                        .map(organization -> organization.isActive())
                        .orElse(false))
                .map(SessionUserResolver::toSessionUser);
    }

    public static SessionUser toSessionUser(User user) {
        return new SessionUser(user.getId(), user.getUsername().value(), user.getFullName(), user.getCompanyId(),
                user.getRole(), user.isOwner(), user.getWarehouseIds(), user.effectivePermissions());
    }
}
