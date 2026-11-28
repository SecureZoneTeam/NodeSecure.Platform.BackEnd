package com.safezone.nodesecure.platform.iam.application.commandservices;

import com.safezone.nodesecure.platform.iam.application.commands.ChangeUserStatusCommand;
import com.safezone.nodesecure.platform.iam.application.commands.CreateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.ChangeOwnPasswordCommand;
import com.safezone.nodesecure.platform.iam.application.commands.DeleteUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateOwnProfileCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignInCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignUpCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateUserPermissionsCommand;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.shared.application.result.Result;

public interface UserCommandService {

    Result<SignInResult, UserCommandFailure> handle(SignInCommand command);

    /** Creates the organization and its owner. Returns the same payload as a sign-in. */
    Result<SignInResult, UserCommandFailure> handle(SignUpCommand command);

    Result<User, UserCommandFailure> handle(CreateUserCommand command);

    Result<User, UserCommandFailure> handle(UpdateUserCommand command);

    Result<User, UserCommandFailure> handle(UpdateUserPermissionsCommand command);

    Result<User, UserCommandFailure> handle(ChangeUserStatusCommand command);

    Result<Long, UserCommandFailure> handle(DeleteUserCommand command);

    /** The signed-in user edits its full name, email or username. */
    Result<User, UserCommandFailure> handle(UpdateOwnProfileCommand command);

    /** The signed-in user changes its password. Returns a new token: tokens issued before stop working. */
    Result<SignInResult, UserCommandFailure> handle(ChangeOwnPasswordCommand command);
}
