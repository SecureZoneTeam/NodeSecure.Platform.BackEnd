package com.safezone.nodesecure.platform.iam.domain.repositories;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    /** Tenant-safe lookup: returns empty when the user belongs to another organization. */
    Optional<User> findByIdAndCompanyId(Long id, Long companyId);

    Optional<User> findByUsername(Username username);

    Optional<User> findByEmail(EmailAddress email);

    List<User> findAllByCompanyId(Long companyId);

    boolean existsById(Long id);

    boolean existsByUsername(Username username);

    boolean existsByEmail(EmailAddress email);

    void deleteById(Long id);

    long count();
}
