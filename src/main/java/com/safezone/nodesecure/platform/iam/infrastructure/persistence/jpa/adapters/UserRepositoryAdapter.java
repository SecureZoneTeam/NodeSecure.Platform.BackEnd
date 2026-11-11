package com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.adapters;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.UserJpaRepository;
import com.safezone.nodesecure.platform.iam.infrastructure.persistence.jpa.mappers.UserPersistenceMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpa;
    private final UserPersistenceMapper mapper;

    public UserRepositoryAdapter(UserJpaRepository jpa, UserPersistenceMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public User save(User user) {
        return mapper.toDomain(jpa.saveAndFlush(mapper.toEntity(user)));
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return jpa.findByUsername(username.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return jpa.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpa.findAll(Sort.by("id")).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsById(Long id) {
        return jpa.existsById(id);
    }

    @Override
    public boolean existsByUsername(Username username) {
        return jpa.existsByUsername(username.value());
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public long count() {
        return jpa.count();
    }
}
