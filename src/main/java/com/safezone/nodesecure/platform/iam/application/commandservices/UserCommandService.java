package com.safezone.nodesecure.platform.iam.application.commandservices;

import com.safezone.nodesecure.platform.iam.application.commands.CreateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.DeleteUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignInCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignUpCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateUserCommand;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.shared.application.result.Result;

public interface UserCommandService {

    Result<SignInResult, UserCommandFailure> handle(SignInCommand command);

    Result<User, UserCommandFailure> handle(SignUpCommand command);

    Result<User, UserCommandFailure> handle(CreateUserCommand command);

    Result<User, UserCommandFailure> handle(UpdateUserCommand command);

    Result<Long, UserCommandFailure> handle(DeleteUserCommand command);
}
