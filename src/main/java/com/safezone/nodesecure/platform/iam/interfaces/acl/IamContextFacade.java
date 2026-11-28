package com.safezone.nodesecure.platform.iam.interfaces.acl;

import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.SessionUser;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

/**
 * Single entry point used by every bounded context to read the authenticated tenant context.
 * The organization, user and warehouse scope always come from the server-side session,
 * never from request parameters.
 */
@Service
public class IamContextFacade {

    private final UserRepository repository;

    public IamContextFacade(UserRepository repository) {
        this.repository = repository;
    }

    public Long currentUserId() {
        return currentSession().map(SessionUser::id).orElse(null);
    }

    public String currentUsername() {
        return currentSession().map(SessionUser::username).orElse(null);
    }

    public Long currentCompanyId() {
        return currentSession().map(SessionUser::companyId).orElse(null);
    }

    /** Organization of the current user; fails with 403 when there is no tenant context. */
    public Long requireCompanyId() {
        Long companyId = currentCompanyId();
        if (companyId == null) {
            throw new AccessDeniedException("No organization in the current session");
        }
        return companyId;
    }

    /** First assigned warehouse (kept for older callers). {@code null} for the owner. */
    public Long currentWarehouseId() {
        return currentSession()
                .filter(session -> !session.allWarehouses())
                .flatMap(session -> session.warehouseIds().stream().findFirst())
                .orElse(null);
    }

    /**
     * Warehouses the current user may reach. {@code null} means every warehouse of the organization (owner);
     * an empty set means none.
     */
    public Set<Long> warehouseScope() {
        // Not Optional.map: a null scope (owner = every warehouse) must not collapse into "no warehouse".
        Optional<SessionUser> session = currentSession();
        return session.isEmpty() ? Set.of() : session.get().warehouseScope();
    }

    /** Scope check only: callers must also verify that the warehouse belongs to the organization. */
    public boolean canAccessWarehouse(Long warehouseId) {
        return currentSession().map(session -> session.canAccessWarehouse(warehouseId)).orElse(false);
    }

    public boolean currentUserHasRole(String role) {
        return currentSession().map(session -> session.hasRole(role)).orElse(false);
    }

    public boolean hasPermission(String permissionCode) {
        return currentSession().map(session -> session.hasPermission(permissionCode)).orElse(false);
    }

    /** Throws 403 when the current user lacks the permission. */
    public void requirePermission(String permissionCode) {
        if (!hasPermission(permissionCode)) {
            throw new AccessDeniedException("Missing permission " + permissionCode);
        }
    }

    /** Users of the organization (owner included), used to check plan limits. */
    public long countUsers(Long companyId) {
        return companyId == null ? 0 : repository.findAllByCompanyId(companyId).size();
    }

    public boolean userExists(Long userId) {
        return userId != null && repository.existsById(userId);
    }

    public Optional<SessionUser> currentSession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SessionUser session) {
            return Optional.of(session);
        }
        return Optional.empty();
    }
}
