package com.safezone.nodesecure.platform.iam.application.internal.commandservices;

import com.safezone.nodesecure.platform.subscriptions.interfaces.acl.SubscriptionContextFacade;
import com.safezone.nodesecure.platform.iam.application.commands.ChangeUserStatusCommand;
import com.safezone.nodesecure.platform.iam.application.commands.ChangeOwnPasswordCommand;
import com.safezone.nodesecure.platform.iam.application.commands.CreateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateOwnProfileCommand;
import com.safezone.nodesecure.platform.iam.application.commands.DeleteUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignInCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignUpCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateUserPermissionsCommand;
import com.safezone.nodesecure.platform.iam.application.commandservices.SignInResult;
import com.safezone.nodesecure.platform.iam.application.commandservices.UserCommandFailure;
import com.safezone.nodesecure.platform.iam.application.commandservices.UserCommandService;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.Organization;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Password;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Permission;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.iam.domain.repositories.OrganizationRepository;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import com.safezone.nodesecure.platform.iam.domain.services.PasswordHasher;
import com.safezone.nodesecure.platform.iam.domain.services.TokenService;
import com.safezone.nodesecure.platform.shared.application.result.Result;
import com.safezone.nodesecure.platform.shared.domain.events.DomainEvent;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseContextFacade;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Account management. Every operation on existing users is scoped to the organization of the
 * authenticated actor: a user of another organization behaves exactly like a missing user.
 */
@Service
public class UserCommandServiceImpl implements UserCommandService {

    private final UserRepository repository;
    private final OrganizationRepository organizationRepository;
    private final PasswordHasher hasher;
    private final TokenService tokenService;
    private final WarehouseContextFacade warehouseFacade;
    private final ApplicationEventPublisher publisher;
    private final SubscriptionContextFacade subscriptionFacade;
    private final Clock clock;

    public UserCommandServiceImpl(UserRepository repository, OrganizationRepository organizationRepository,
                                  PasswordHasher hasher, TokenService tokenService,
                                  WarehouseContextFacade warehouseFacade, ApplicationEventPublisher publisher,
                                  SubscriptionContextFacade subscriptionFacade, Clock clock) {
        this.subscriptionFacade = subscriptionFacade;
        this.repository = repository;
        this.organizationRepository = organizationRepository;
        this.hasher = hasher;
        this.tokenService = tokenService;
        this.warehouseFacade = warehouseFacade;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Result<SignInResult, UserCommandFailure> handle(SignInCommand command) {
        Optional<User> found = findByUsername(command.username());
        // The password is checked first so a wrong password never reveals the account status.
        if (found.isEmpty() || command.password() == null
                || !hasher.matches(command.password(), found.get().getPasswordHash())) {
            return Result.failure(new UserCommandFailure.InvalidCredentials());
        }
        User user = found.get();
        if (!user.isActive()) {
            return Result.failure(new UserCommandFailure.AccountSuspended());
        }
        Optional<Organization> organization = organizationRepository.findById(user.getCompanyId());
        if (organization.isEmpty() || !organization.get().isActive()) {
            return Result.failure(new UserCommandFailure.Forbidden("iam.error.organization.unavailable"));
        }
        return Result.success(new SignInResult(user, organization.get(), tokenService.createToken(user)));
    }

    @Override
    @Transactional
    public Result<SignInResult, UserCommandFailure> handle(SignUpCommand command) {
        Username username = new Username(command.username());
        EmailAddress email = new EmailAddress(command.email());
        Password password = new Password(command.password());
        if (repository.existsByUsername(username)) {
            return Result.failure(new UserCommandFailure.UsernameTaken());
        }
        if (repository.existsByEmail(email)) {
            return Result.failure(new UserCommandFailure.EmailTaken());
        }
        Instant now = clock.instant();
        // The organization is always new: the client can never choose an existing one.
        Organization organization = organizationRepository.save(Organization.create(command.organizationName(), now));
        User owner = User.registerOwner(username, email, command.fullName(), hasher.hash(password.value()),
                organization.getId(), now);
        User savedOwner = persist(owner);
        organization.assignOwner(savedOwner.getId(), now);
        Organization savedOrganization = organizationRepository.save(organization);
        return Result.success(new SignInResult(savedOwner, savedOrganization, tokenService.createToken(savedOwner)));
    }

    @Override
    @Transactional
    public Result<User, UserCommandFailure> handle(CreateUserCommand command) {
        if (command.companyId() == null) {
            return forbidden("iam.error.organization.unavailable");
        }
        Username username = new Username(command.username());
        EmailAddress email = new EmailAddress(command.email());
        Password password = new Password(command.password());
        Role role = Role.from(command.role());
        if (role.isOwner()) {
            return validation("iam.error.role.ownerNotAssignable");
        }
        Optional<Set<Long>> warehouses = checkWarehouses(command.warehouseIds(), command.companyId());
        if (warehouses.isEmpty()) {
            return validation("iam.error.warehouses.invalid");
        }
        if (repository.existsByUsername(username)) {
            return Result.failure(new UserCommandFailure.UsernameTaken());
        }
        if (repository.existsByEmail(email)) {
            return Result.failure(new UserCommandFailure.EmailTaken());
        }
        subscriptionFacade.requireUserCapacity(command.companyId());
        User worker = User.createWorker(username, email, command.fullName(), hasher.hash(password.value()),
                role, command.companyId(), warehouses.get(), clock.instant());
        return Result.success(persist(worker));
    }

    @Override
    @Transactional
    public Result<User, UserCommandFailure> handle(UpdateUserCommand command) {
        Optional<User> found = repository.findByIdAndCompanyId(command.id(), command.companyId());
        if (found.isEmpty()) {
            return notFound(command.id());
        }
        User user = found.get();
        boolean changesAccess = command.role() != null || command.warehouseIds() != null;
        if (changesAccess && user.isOwner()) {
            return forbidden("iam.error.owner.immutable");
        }
        if (changesAccess && Objects.equals(user.getId(), command.actorUserId())) {
            return forbidden("iam.error.self.accessChange");
        }
        EmailAddress newEmail = isBlank(command.email()) ? null : new EmailAddress(command.email());
        if (newEmail != null && !newEmail.equals(user.getEmail()) && repository.existsByEmail(newEmail)) {
            return Result.failure(new UserCommandFailure.EmailTaken());
        }
        Instant now = clock.instant();
        user.updateProfile(newEmail, command.fullName(), now);
        if (command.role() != null) {
            Role role = Role.from(command.role());
            if (role.isOwner()) {
                return validation("iam.error.role.ownerNotAssignable");
            }
            user.changeRole(role, now);
        }
        if (command.warehouseIds() != null) {
            Optional<Set<Long>> warehouses = checkWarehouses(command.warehouseIds(), command.companyId());
            if (warehouses.isEmpty()) {
                return validation("iam.error.warehouses.invalid");
            }
            user.assignWarehouses(warehouses.get(), now);
        }
        return Result.success(repository.save(user));
    }

    @Override
    @Transactional
    public Result<User, UserCommandFailure> handle(UpdateUserPermissionsCommand command) {
        Optional<User> found = repository.findByIdAndCompanyId(command.id(), command.companyId());
        if (found.isEmpty()) {
            return notFound(command.id());
        }
        User user = found.get();
        Optional<UserCommandFailure> protectedTarget = protectedTarget(user, command.actorUserId());
        if (protectedTarget.isPresent()) {
            return Result.failure(protectedTarget.get());
        }
        Map<Permission, Boolean> overrides = new EnumMap<>(Permission.class);
        if (command.overrides() != null) {
            for (Map.Entry<String, Boolean> entry : command.overrides().entrySet()) {
                Optional<Permission> permission = Permission.fromCode(entry.getKey());
                if (permission.isEmpty() || entry.getValue() == null) {
                    return validation("iam.error.permission.invalid");
                }
                if (permission.get().ownerOnly() && entry.getValue()) {
                    return forbidden("iam.error.permission.ownerOnly");
                }
                overrides.put(permission.get(), entry.getValue());
            }
        }
        user.replacePermissionOverrides(overrides, clock.instant());
        return Result.success(repository.save(user));
    }

    @Override
    @Transactional
    public Result<User, UserCommandFailure> handle(ChangeUserStatusCommand command) {
        Optional<User> found = repository.findByIdAndCompanyId(command.id(), command.companyId());
        if (found.isEmpty()) {
            return notFound(command.id());
        }
        User user = found.get();
        Optional<UserCommandFailure> protectedTarget = protectedTarget(user, command.actorUserId());
        if (protectedTarget.isPresent()) {
            return Result.failure(protectedTarget.get());
        }
        if (command.suspend()) {
            user.suspend(clock.instant());
        } else {
            user.reactivate(clock.instant());
        }
        return Result.success(repository.save(user));
    }

    @Override
    @Transactional
    public Result<Long, UserCommandFailure> handle(DeleteUserCommand command) {
        Optional<User> found = repository.findByIdAndCompanyId(command.id(), command.companyId());
        if (found.isEmpty()) {
            return notFound(command.id());
        }
        Optional<UserCommandFailure> protectedTarget = protectedTarget(found.get(), command.actorUserId());
        if (protectedTarget.isPresent()) {
            return Result.failure(protectedTarget.get());
        }
        repository.deleteById(command.id());
        return Result.success(command.id());
    }

    /** The owner and the actor's own account cannot be suspended, deleted or have their permissions changed. */
    @Override
    @Transactional
    public Result<User, UserCommandFailure> handle(UpdateOwnProfileCommand command) {
        Optional<User> found = repository.findByIdAndCompanyId(command.userId(), command.companyId());
        if (found.isEmpty()) {
            return notFound(command.userId());
        }
        User user = found.get();
        EmailAddress newEmail = isBlank(command.email()) ? null : new EmailAddress(command.email());
        if (newEmail != null && !newEmail.equals(user.getEmail()) && repository.existsByEmail(newEmail)) {
            return Result.failure(new UserCommandFailure.EmailTaken());
        }
        Username newUsername = isBlank(command.username()) ? null : new Username(command.username());
        if (newUsername != null && !newUsername.equals(user.getUsername()) && repository.existsByUsername(newUsername)) {
            return Result.failure(new UserCommandFailure.UsernameTaken());
        }
        Instant now = clock.instant();
        user.updateProfile(newEmail, isBlank(command.fullName()) ? null : command.fullName(), now);
        if (newUsername != null && !newUsername.equals(user.getUsername())) {
            user.changeUsername(newUsername, now);
        }
        return Result.success(repository.save(user));
    }

    @Override
    @Transactional
    public Result<SignInResult, UserCommandFailure> handle(ChangeOwnPasswordCommand command) {
        Optional<User> found = repository.findByIdAndCompanyId(command.userId(), command.companyId());
        if (found.isEmpty()) {
            return notFound(command.userId());
        }
        User user = found.get();
        if (command.currentPassword() == null || !hasher.matches(command.currentPassword(), user.getPasswordHash())) {
            return validation("iam.error.password.current");
        }
        Password password = new Password(command.newPassword());
        user.changePassword(hasher.hash(password.value()), clock.instant());
        User saved = repository.save(user);
        Optional<Organization> organization = organizationRepository.findById(saved.getCompanyId());
        if (organization.isEmpty()) {
            return forbidden("iam.error.organization.unavailable");
        }
        return Result.success(new SignInResult(saved, organization.get(), tokenService.createToken(saved)));
    }

    private Optional<UserCommandFailure> protectedTarget(User target, Long actorUserId) {
        if (target.isOwner()) {
            return Optional.of(new UserCommandFailure.Forbidden("iam.error.owner.immutable"));
        }
        if (Objects.equals(target.getId(), actorUserId)) {
            return Optional.of(new UserCommandFailure.Forbidden("iam.error.self.accessChange"));
        }
        return Optional.empty();
    }

    /** Every warehouse must exist and belong to the organization of the actor. */
    private Optional<Set<Long>> checkWarehouses(List<Long> warehouseIds, Long companyId) {
        if (warehouseIds == null || warehouseIds.isEmpty() || warehouseIds.stream().anyMatch(Objects::isNull)) {
            return Optional.empty();
        }
        Set<Long> ids = new LinkedHashSet<>(warehouseIds);
        return warehouseFacade.allWarehousesBelongToCompany(ids, companyId) ? Optional.of(ids) : Optional.empty();
    }

    private User persist(User user) {
        List<DomainEvent> events = user.pullDomainEvents();
        User saved = repository.save(user);
        events.forEach(publisher::publishEvent);
        return saved;
    }

    private Optional<User> findByUsername(String username) {
        try {
            return repository.findByUsername(new Username(username));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static <T> Result<T, UserCommandFailure> notFound(Long id) {
        return Result.failure(new UserCommandFailure.NotFound(id));
    }

    private static <T> Result<T, UserCommandFailure> forbidden(String key) {
        return Result.failure(new UserCommandFailure.Forbidden(key));
    }

    private static <T> Result<T, UserCommandFailure> validation(String key) {
        return Result.failure(new UserCommandFailure.Validation(key));
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
