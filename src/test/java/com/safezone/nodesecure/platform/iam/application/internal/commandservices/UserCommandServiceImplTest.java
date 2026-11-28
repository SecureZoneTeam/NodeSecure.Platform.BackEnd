package com.safezone.nodesecure.platform.iam.application.internal.commandservices;

import com.safezone.nodesecure.platform.iam.application.commands.ChangeOwnPasswordCommand;
import com.safezone.nodesecure.platform.iam.application.commands.ChangeUserStatusCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateOwnProfileCommand;
import com.safezone.nodesecure.platform.iam.application.commands.CreateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.DeleteUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignInCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignUpCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateUserPermissionsCommand;
import com.safezone.nodesecure.platform.iam.application.commandservices.SignInResult;
import com.safezone.nodesecure.platform.iam.application.commandservices.UserCommandFailure;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.Organization;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.events.UserRegisteredEvent;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Permission;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.TokenClaims;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.iam.domain.repositories.OrganizationRepository;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import com.safezone.nodesecure.platform.iam.domain.services.PasswordHasher;
import com.safezone.nodesecure.platform.iam.domain.services.TokenService;
import com.safezone.nodesecure.platform.shared.application.result.Result;
import com.safezone.nodesecure.platform.shared.domain.exceptions.BusinessRuleViolationException;
import com.safezone.nodesecure.platform.subscriptions.interfaces.acl.SubscriptionContextFacade;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseContextFacade;
import com.safezone.nodesecure.platform.warehouses.interfaces.acl.WarehouseZoneConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class UserCommandServiceImplTest {

    private FakeUserRepository repository;
    private FakeOrganizationRepository organizations;
    private List<Object> publishedEvents;
    private UserCommandServiceImpl service;
    /** warehouseId -> companyId */
    private final Map<Long, Long> warehouses = new HashMap<>();

    private final FakeSubscriptionFacade subscriptions = new FakeSubscriptionFacade();

    @BeforeEach
    void setUp() {
        subscriptions.userLimitReached = false;
        repository = new FakeUserRepository();
        organizations = new FakeOrganizationRepository();
        publishedEvents = new ArrayList<>();
        warehouses.clear();
        Clock clock = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC);
        service = new UserCommandServiceImpl(repository, organizations, new FakeHasher(), new FakeTokenService(),
                new FakeWarehouseFacade(), publishedEvents::add, subscriptions, clock);
    }

    private SignInResult signUp(String username, String organizationName) {
        return service.handle(new SignUpCommand(username, username + "@mail.pe", "secret123",
                "Owner " + username, organizationName)).success().orElseThrow();
    }

    private User createWorker(SignInResult owner, String username, List<Long> warehouseIds) {
        return service.handle(new CreateUserCommand(owner.user().getCompanyId(), owner.user().getId(), username,
                username + "@mail.pe", "secret123", "Worker " + username, "ROLE_OPERATOR", warehouseIds))
                .success().orElseThrow();
    }

    @Test
    void signUpCreatesANewOrganizationWithItsOwner() {
        SignInResult result = signUp("ownera", "Organización A");

        assertEquals("Organización A", result.organization().getName());
        assertEquals(result.user().getId(), result.organization().getOwnerUserId());
        assertEquals(result.organization().getId(), result.user().getCompanyId());
        assertEquals(Role.ROLE_OWNER, result.user().getRole());
        assertEquals("hashed:secret123", result.user().getPasswordHash());
        assertEquals("token-for-ownera", result.token());
        assertInstanceOf(UserRegisteredEvent.class, publishedEvents.get(0));
    }

    @Test
    void everySignUpGetsItsOwnOrganization() {
        SignInResult ownerA = signUp("ownera", "Organización A");
        SignInResult ownerB = signUp("ownerb", "Organización B");

        assertNotEquals(ownerA.user().getCompanyId(), ownerB.user().getCompanyId());
    }

    @Test
    void signUpRejectsRepeatedUsername() {
        signUp("ownera", "Organización A");

        Result<SignInResult, UserCommandFailure> result = service.handle(
                new SignUpCommand("OwnerA", "otro@mail.pe", "secret123", "Owner", "Otra"));

        assertInstanceOf(UserCommandFailure.UsernameTaken.class, result.failure().orElseThrow());
    }

    @Test
    void signInChecksPasswordAndStatus() {
        SignInResult owner = signUp("ownera", "Organización A");
        warehouses.put(10L, owner.user().getCompanyId());
        User worker = createWorker(owner, "pepito", List.of(10L));

        assertTrue(service.handle(new SignInCommand("pepito", "secret123")).isSuccess());
        assertInstanceOf(UserCommandFailure.InvalidCredentials.class,
                service.handle(new SignInCommand("pepito", "wrong-pass")).failure().orElseThrow());

        service.handle(new ChangeUserStatusCommand(owner.user().getCompanyId(), owner.user().getId(), worker.getId(), true));
        assertInstanceOf(UserCommandFailure.AccountSuspended.class,
                service.handle(new SignInCommand("pepito", "secret123")).failure().orElseThrow());
    }

    @Test
    void ownerCreatesActiveWorkerInItsOwnOrganization() {
        SignInResult owner = signUp("ownera", "Organización A");
        warehouses.put(10L, owner.user().getCompanyId());

        User worker = createWorker(owner, "pepito", List.of(10L));

        assertEquals(owner.user().getCompanyId(), worker.getCompanyId());
        assertEquals(Set.of(10L), worker.getWarehouseIds());
        assertTrue(worker.isActive());
        assertEquals("hashed:secret123", worker.getPasswordHash());
    }

    @Test
    void workerCannotBeAssignedToAWarehouseOfAnotherOrganization() {
        SignInResult ownerA = signUp("ownera", "Organización A");
        SignInResult ownerB = signUp("ownerb", "Organización B");
        warehouses.put(30L, ownerB.user().getCompanyId());

        var result = service.handle(new CreateUserCommand(ownerA.user().getCompanyId(), ownerA.user().getId(),
                "pepito", "pepito@mail.pe", "secret123", "Pepito", "ROLE_OPERATOR", List.of(30L)));

        assertInstanceOf(UserCommandFailure.Validation.class, result.failure().orElseThrow());
    }

    @Test
    void ownerRoleCannotBeAssignedToAWorker() {
        SignInResult owner = signUp("ownera", "Organización A");
        warehouses.put(10L, owner.user().getCompanyId());

        var result = service.handle(new CreateUserCommand(owner.user().getCompanyId(), owner.user().getId(),
                "pepito", "pepito@mail.pe", "secret123", "Pepito", "ROLE_OWNER", List.of(10L)));

        assertInstanceOf(UserCommandFailure.Validation.class, result.failure().orElseThrow());
    }

    @Test
    void usersOfAnotherOrganizationBehaveAsMissing() {
        SignInResult ownerA = signUp("ownera", "Organización A");
        SignInResult ownerB = signUp("ownerb", "Organización B");
        warehouses.put(10L, ownerA.user().getCompanyId());
        User pepito = createWorker(ownerA, "pepito", List.of(10L));
        Long companyB = ownerB.user().getCompanyId();
        Long actorB = ownerB.user().getId();

        assertInstanceOf(UserCommandFailure.NotFound.class, service.handle(new UpdateUserCommand(companyB, actorB,
                pepito.getId(), null, "Hacked", "ROLE_SUPERVISOR", null)).failure().orElseThrow());
        assertInstanceOf(UserCommandFailure.NotFound.class, service.handle(new ChangeUserStatusCommand(companyB, actorB,
                pepito.getId(), true)).failure().orElseThrow());
        assertInstanceOf(UserCommandFailure.NotFound.class, service.handle(new DeleteUserCommand(companyB, actorB,
                pepito.getId())).failure().orElseThrow());
        assertTrue(repository.findById(pepito.getId()).orElseThrow().isActive());
    }

    @Test
    void permissionOverridesAreValidated() {
        SignInResult owner = signUp("ownera", "Organización A");
        warehouses.put(10L, owner.user().getCompanyId());
        User pepito = createWorker(owner, "pepito", List.of(10L));
        Long company = owner.user().getCompanyId();

        User updated = service.handle(new UpdateUserPermissionsCommand(company, owner.user().getId(), pepito.getId(),
                Map.of("inventory.exit.create", false))).success().orElseThrow();
        assertFalse(updated.effectivePermissions().contains(Permission.EXIT_CREATE));

        assertInstanceOf(UserCommandFailure.Forbidden.class, service.handle(new UpdateUserPermissionsCommand(company,
                owner.user().getId(), pepito.getId(), Map.of("users.create", true))).failure().orElseThrow());
        assertInstanceOf(UserCommandFailure.Validation.class, service.handle(new UpdateUserPermissionsCommand(company,
                owner.user().getId(), pepito.getId(), Map.of("does.not.exist", true))).failure().orElseThrow());
    }

    @Test
    void ownerAndOwnAccountAreProtected() {
        SignInResult owner = signUp("ownera", "Organización A");
        Long company = owner.user().getCompanyId();
        Long ownerId = owner.user().getId();

        assertInstanceOf(UserCommandFailure.Forbidden.class,
                service.handle(new ChangeUserStatusCommand(company, ownerId, ownerId, true)).failure().orElseThrow());
        assertInstanceOf(UserCommandFailure.Forbidden.class,
                service.handle(new DeleteUserCommand(company, ownerId, ownerId)).failure().orElseThrow());
        assertInstanceOf(UserCommandFailure.Forbidden.class, service.handle(new UpdateUserCommand(company, ownerId,
                ownerId, null, null, "ROLE_OPERATOR", null)).failure().orElseThrow());
    }

    @Test
    void suspensionInvalidatesPreviousTokens() {
        SignInResult owner = signUp("ownera", "Organización A");
        warehouses.put(10L, owner.user().getCompanyId());
        User pepito = createWorker(owner, "pepito", List.of(10L));
        long versionBefore = pepito.getTokenVersion();

        User suspended = service.handle(new ChangeUserStatusCommand(owner.user().getCompanyId(), owner.user().getId(),
                pepito.getId(), true)).success().orElseThrow();

        assertEquals(versionBefore + 1, suspended.getTokenVersion());
        assertFalse(suspended.isActive());
    }

    @Test
    void workerCreationIsRejectedWhenThePlanUserLimitIsReached() {
        SignInResult owner = signUp("ownera", "Organización A");
        warehouses.put(10L, owner.user().getCompanyId());
        subscriptions.userLimitReached = true;

        assertThrows(BusinessRuleViolationException.class, () -> createWorker(owner, "pepito", List.of(10L)));
        assertEquals(1, repository.findAllByCompanyId(owner.user().getCompanyId()).size());
    }

    @Test
    void userEditsOwnProfileWithUniqueEmailAndUsername() {
        SignInResult owner = signUp("ownera", "Organización A");
        signUp("ownerb", "Organización B");
        Long companyId = owner.user().getCompanyId();
        Long id = owner.user().getId();

        Result<User, UserCommandFailure> taken = service.handle(new UpdateOwnProfileCommand(id, companyId, null, null, "ownerb"));
        assertInstanceOf(UserCommandFailure.UsernameTaken.class, taken.failure().orElseThrow());
        Result<User, UserCommandFailure> emailTaken = service.handle(
                new UpdateOwnProfileCommand(id, companyId, null, "ownerb@mail.pe", null));
        assertInstanceOf(UserCommandFailure.EmailTaken.class, emailTaken.failure().orElseThrow());

        User updated = service.handle(new UpdateOwnProfileCommand(id, companyId, "Juan Quispe", "juan@mail.pe", "jquispe"))
                .success().orElseThrow();
        assertEquals("Juan Quispe", updated.getFullName());
        assertEquals("juan@mail.pe", updated.getEmail().value());
        assertEquals("jquispe", updated.getUsername().value());
        assertTrue(service.handle(new SignInCommand("jquispe", "secret123")).success().isPresent());
    }

    @Test
    void passwordChangeRequiresTheCurrentPasswordAndRevokesOldTokens() {
        SignInResult owner = signUp("ownera", "Organización A");
        Long companyId = owner.user().getCompanyId();
        Long id = owner.user().getId();
        long versionBefore = owner.user().getTokenVersion();

        Result<SignInResult, UserCommandFailure> wrong = service.handle(
                new ChangeOwnPasswordCommand(id, companyId, "not-my-password", "newSecret456"));
        assertInstanceOf(UserCommandFailure.Validation.class, wrong.failure().orElseThrow());

        SignInResult changed = service.handle(new ChangeOwnPasswordCommand(id, companyId, "secret123", "newSecret456"))
                .success().orElseThrow();
        assertEquals("hashed:newSecret456", changed.user().getPasswordHash());
        assertEquals(versionBefore + 1, changed.user().getTokenVersion());
        assertNotNull(changed.token());
        assertTrue(service.handle(new SignInCommand("ownera", "secret123")).failure().isPresent());
        assertTrue(service.handle(new SignInCommand("ownera", "newSecret456")).success().isPresent());
    }

    private static final class FakeSubscriptionFacade implements SubscriptionContextFacade {
        boolean userLimitReached;

        public void requireWarehouseCapacity(Long companyId) { }
        public void requireSensorNodeCapacity(Long companyId) { }
        public void requireUserCapacity(Long companyId) {
            if (userLimitReached) {
                throw new BusinessRuleViolationException("subscriptions.error.limitReached.users");
            }
        }
    }

    private final class FakeWarehouseFacade implements WarehouseContextFacade {
        public boolean zoneBelongsToCompany(Long zoneId, Long companyId) { return false; }
        public Optional<WarehouseZoneConfiguration> findZoneConfiguration(Long zoneId, Long companyId) { return Optional.empty(); }
        public Set<Long> findZoneIdsByWarehouseId(Long warehouseId, Long companyId) { return Set.of(); }
        public Set<Long> findZoneIdsByWarehouseIds(Collection<Long> ids, Long companyId) { return Set.of(); }
        public boolean warehouseBelongsToCompany(Long warehouseId, Long companyId) {
            return companyId != null && companyId.equals(warehouses.get(warehouseId));
        }
        public Set<Long> findWarehouseIdsByCompanyId(Long companyId) { return Set.of(); }
        public boolean allWarehousesBelongToCompany(Collection<Long> ids, Long companyId) {
            return ids.stream().allMatch(id -> warehouseBelongsToCompany(id, companyId));
        }

        @Override
        public Optional<String> findWarehouseName(Long warehouseId, Long companyId) {
            return Optional.empty();
        }
    }

    private static final class FakeHasher implements PasswordHasher {
        public String hash(String password) { return "hashed:" + password; }
        public boolean matches(String password, String hash) { return hash.equals("hashed:" + password); }
    }

    private static final class FakeTokenService implements TokenService {
        public String createToken(User user) { return "token-for-" + user.getUsername().value(); }
        public Optional<TokenClaims> readToken(String token) { return Optional.empty(); }
    }

    private static final class FakeOrganizationRepository implements OrganizationRepository {
        private final Map<Long, Organization> rows = new LinkedHashMap<>();
        private long nextId = 1;

        public Organization save(Organization organization) {
            Long id = organization.getId() != null ? organization.getId() : nextId++;
            Organization saved = Organization.restore(id, organization.getName(), organization.getOwnerUserId(),
                    organization.getStatus(), organization.getCreatedAt(), organization.getUpdatedAt());
            rows.put(id, saved);
            return saved;
        }

        public Optional<Organization> findById(Long id) { return Optional.ofNullable(rows.get(id)); }
    }

    private static final class FakeUserRepository implements UserRepository {
        private final Map<Long, User> rows = new LinkedHashMap<>();
        private long nextId = 1;

        public User save(User user) {
            Long id = user.getId() != null ? user.getId() : nextId++;
            User saved = User.restore(id, user.getUsername(), user.getEmail(), user.getFullName(),
                    user.getPasswordHash(), user.getRole(), user.getStatus(), user.getTokenVersion(),
                    user.getCompanyId(), user.getWarehouseIds(), user.getPermissionOverrides(),
                    user.getCreatedAt(), user.getUpdatedAt());
            rows.put(id, saved);
            return saved;
        }

        public Optional<User> findById(Long id) { return Optional.ofNullable(rows.get(id)); }
        public Optional<User> findByIdAndCompanyId(Long id, Long companyId) {
            return findById(id).filter(user -> user.belongsTo(companyId));
        }
        public Optional<User> findByUsername(Username username) {
            return rows.values().stream().filter(u -> u.getUsername().equals(username)).findFirst();
        }
        public Optional<User> findByEmail(EmailAddress email) {
            return rows.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
        }
        public List<User> findAllByCompanyId(Long companyId) {
            return rows.values().stream().filter(u -> u.belongsTo(companyId)).toList();
        }
        public boolean existsById(Long id) { return rows.containsKey(id); }
        public boolean existsByUsername(Username username) { return findByUsername(username).isPresent(); }
        public boolean existsByEmail(EmailAddress email) { return findByEmail(email).isPresent(); }
        public void deleteById(Long id) { rows.remove(id); }
        public long count() { return rows.size(); }
    }
}
