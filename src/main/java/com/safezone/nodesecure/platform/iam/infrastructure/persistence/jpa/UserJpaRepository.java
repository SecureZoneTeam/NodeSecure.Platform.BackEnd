package com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa;

import com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.entities.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByUsername(String username);

    Optional<UserJpaEntity> findByEmail(String email);

    Optional<UserJpaEntity> findByIdAndCompanyId(Long id, Long companyId);

    List<UserJpaEntity> findAllByCompanyIdOrderByIdAsc(Long companyId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
