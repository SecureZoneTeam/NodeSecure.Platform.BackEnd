package com.safezone.nodesecure.platform.iam.application.internal.commandservices;

import com.safezone.nodesecure.platform.iam.application.commands.CreateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.DeleteUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignInCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignUpCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commandservices.SignInResult;
import com.safezone.nodesecure.platform.iam.application.commandservices.UserCommandFailure;
import com.safezone.nodesecure.platform.iam.application.commandservices.UserCommandService;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Password;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import com.safezone.nodesecure.platform.iam.domain.services.PasswordHasher;
import com.safezone.nodesecure.platform.iam.domain.services.TokenService;
import com.safezone.nodesecure.platform.shared.application.result.Result;
import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository repository;
    private final PasswordHasher hasher;
    private final TokenService tokenService;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;
    private final String defaultPassword;

    public UserCommandServiceImpl(UserRepository repository, PasswordHasher hasher, TokenService tokenService,
                                  ApplicationEventPublisher publisher, Clock clock,
                                  @Value("${iam.default-password}") String defaultPassword) {
        this.repository = repository;
        this.hasher = hasher;
        this.tokenService = tokenService;
        this.publisher = publisher;
        this.clock = clock;
        this.defaultPassword = defaultPassword;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<SignInResult, UserCommandFailure> handle(SignInCommand command) {
        Optional<User> found = findByUsername(command.username());
        if (found.isEmpty() || command.password() == null
                || !hasher.matches(command.password(), found.get().getPasswordHash())) {
            return Result.failure(new UserCommandFailure.InvalidCredentials());
        }
        User user = found.get();
        return Result.success(new SignInResult(user, tokenService.createToken(user)));
    }

    @Override
    @Transactional
    public Result<User, UserCommandFailure> handle(SignUpCommand command) {
        return register(
                new Username(command.username()),
                new EmailAddress(command.email()),
                new Password(command.password()),
                command.fullName(),
                EnumSet.of(Role.ROLE_WAREHOUSE_KEEPER),
                command.companyId(),
                null);
    }

    @Override
    @Transactional
    public Result<User, UserCommandFailure> handle(CreateUserCommand command) {
        String password = isBlank(command.password()) ? defaultPassword : command.password();
        Set<Role> roles = command.roles() == null || command.roles().isEmpty()
                ? EnumSet.of(Role.ROLE_WAREHOUSE_KEEPER)
                : toRoles(command.roles());
        return register(
                new Username(command.username()),
                new EmailAddress(command.email()),
                new Password(password),
                command.fullName(),
                roles,
                command.companyId(),
                command.warehouseId());
    }

    @Override
    @Transactional
    public Result<User, UserCommandFailure> handle(UpdateUserCommand command) {
        Optional<User> found = repository.findById(command.id());
        if (found.isEmpty()) {
            return Result.failure(new UserCommandFailure.NotFound(command.id()));
        }
        User user = found.get();

        EmailAddress newEmail = isBlank(command.email()) ? null : new EmailAddress(command.email());
        if (newEmail != null && !newEmail.equals(user.getEmail()) && repository.existsByEmail(newEmail)) {
            return Result.failure(new UserCommandFailure.EmailTaken());
        }
        Set<Role> newRoles = command.roles() == null ? null : toRoles(command.roles());

        user.update(newEmail, command.fullName(), newRoles, command.warehouseId(), clock.instant());
        return Result.success(repository.save(user));
    }

    @Override
    @Transactional
    public Result<Long, UserCommandFailure> handle(DeleteUserCommand command) {
        if (!repository.existsById(command.id())) {
            return Result.failure(new UserCommandFailure.NotFound(command.id()));
        }
        repository.deleteById(command.id());
        return Result.success(command.id());
    }

    private Result<User, UserCommandFailure> register(Username username, EmailAddress email, Password password,
                                                      String fullName, Set<Role> roles, Long companyId,
                                                      Long warehouseId) {
        if (repository.existsByUsername(username)) {
            return Result.failure(new UserCommandFailure.UsernameTaken());
        }
        if (repository.existsByEmail(email)) {
            return Result.failure(new UserCommandFailure.EmailTaken());
        }
        User user = User.create(username, email, fullName, hasher.hash(password.value()),
                roles, companyId, warehouseId, clock.instant());
        List<DomainEvent> events = user.pullDomainEvents();
        User saved = repository.save(user);
        events.forEach(publisher::publishEvent);
        return Result.success(saved);
    }

    private Optional<User> findByUsername(String username) {
        try {
            return repository.findByUsername(new Username(username));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private Set<Role> toRoles(List<String> names) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (String name : names) {
            roles.add(Role.from(name));
        }
        return roles;
    }

    private boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
