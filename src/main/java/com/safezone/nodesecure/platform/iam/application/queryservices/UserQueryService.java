package com.safezone.nodesecure.platform.iam.application.queryservices;

import com.safezone.nodesecure.platform.iam.application.queries.GetAllUsersQuery;
import com.safezone.nodesecure.platform.iam.application.queries.GetUserByIdQuery;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;

import java.util.List;
import java.util.Optional;

public interface UserQueryService {

    List<User> handle(GetAllUsersQuery query);

    Optional<User> handle(GetUserByIdQuery query);
}
