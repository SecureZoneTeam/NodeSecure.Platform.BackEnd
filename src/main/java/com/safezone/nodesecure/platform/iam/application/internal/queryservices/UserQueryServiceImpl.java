package com.safezone.nodesecure.platform.iam.application.internal.queryservices;

import com.safezone.nodesecure.platform.iam.application.queries.GetAllUsersQuery;
import com.safezone.nodesecure.platform.iam.application.queries.GetUserByIdQuery;
import com.safezone.nodesecure.platform.iam.application.queryservices.UserQueryService;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserQueryServiceImpl implements UserQueryService {

    private final UserRepository repository;

    public UserQueryServiceImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<User> handle(GetAllUsersQuery query) {
        return repository.findAll();
    }

    @Override
    public Optional<User> handle(GetUserByIdQuery query) {
        return repository.findById(query.id());
    }
}
