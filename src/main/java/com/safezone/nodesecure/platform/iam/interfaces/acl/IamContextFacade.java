package com.safezone.nodesecure.platform.iam.interfaces.acl;

import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.SessionUser;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    public Long currentWarehouseId() {
        return currentSession().map(SessionUser::warehouseId).orElse(null);
    }

    public boolean currentUserHasRole(String role) {
        return currentSession().map(session -> session.hasRole(role)).orElse(false);
    }

    public boolean userExists(Long userId) {
        return userId != null && repository.existsById(userId);
    }

    private Optional<SessionUser> currentSession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof SessionUser session) {
            return Optional.of(session);
        }
        return Optional.empty();
    }
}
