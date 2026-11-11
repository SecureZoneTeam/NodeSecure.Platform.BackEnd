package com.safezone.nodesecure.platform.iam.application.internal.commandservices;

import com.safezone.nodesecure.platform.iam.application.commands.CreateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.DeleteUserCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignInCommand;
import com.safezone.nodesecure.platform.iam.application.commands.SignUpCommand;
import com.safezone.nodesecure.platform.iam.application.commands.UpdateUserCommand;
import com.safezone.nodesecure.platform.iam.application.commandservices.SignInResult;
import com.safezone.nodesecure.platform.iam.application.commandservices.UserCommandFailure;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.events.UserRegisteredEvent;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.SessionUser;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import com.safezone.nodesecure.platform.iam.domain.services.PasswordHasher;
import com.safezone.nodesecure.platform.iam.domain.services.TokenService;
import com.safezone.nodesecure.platform.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserCommandServiceImplTest {

    private FakeUserRepository repository;
    private List<Object> publishedEvents;
    private UserCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new FakeUserRepository();
        publishedEvents = new ArrayList<>();
        Clock clock = Clock.fixed(Instant.parse("2026-11-11T10:00:00Z"), ZoneOffset.UTC);
        service = new UserCommandServiceImpl(repository, new FakeHasher(), new FakeTokenService(),
                publishedEvents::add, clock, "12345678");
    }

    @Test
    void signUpCreatesWarehouseKeeperAndPublishesEvent() {
        Result<User, UserCommandFailure> result = service.handle(
                new SignUpCommand("jquispe", "juan.quispe@andina.pe", "secret123", "Juan Quispe", 1L));

        User user = result.success().orElseThrow();
        assertEquals(1L, user.getId());
        assertTrue(user.hasRole(Role.ROLE_WAREHOUSE_KEEPER));
        assertEquals("hashed:secret123", user.getPasswordHash());
        assertEquals(1, publishedEvents.size());
        assertTrue(publishedEvents.get(0) instanceof UserRegisteredEvent);
    }

    @Test
    void signUpRejectsRepeatedUsername() {
        service.handle(new SignUpCommand("jquispe", "juan.quispe@andina.pe", "secret123", "Juan Quispe", 1L));

        Result<User, UserCommandFailure> result = service.handle(
                new SignUpCommand("JQuispe", "otro@andina.pe", "secret123", "Juan Quispe", 1L));

        assertTrue(result.failure().orElseThrow() instanceof UserCommandFailure.UsernameTaken);
    }

    @Test
    void signInReturnsTokenWithRightPassword() {
        service.handle(new SignUpCommand("jquispe", "juan.quispe@andina.pe", "secret123", "Juan Quispe", 1L));

        Result<SignInResult, UserCommandFailure> result = service.handle(new SignInCommand("jquispe", "secret123"));

        assertEquals("token-for-jquispe", result.success().orElseThrow().token());
    }

    @Test
    void signInFailsWithWrongPassword() {
        service.handle(new SignUpCommand("jquispe", "juan.quispe@andina.pe", "secret123", "Juan Quispe", 1L));

        Result<SignInResult, UserCommandFailure> result = service.handle(new SignInCommand("jquispe", "wrongpass"));

        assertTrue(result.failure().orElseThrow() instanceof UserCommandFailure.InvalidCredentials);
    }

    @Test
    void signInFailsWithUnknownOrInvalidUsername() {
        assertTrue(service.handle(new SignInCommand("nobody", "secret123")).isFailure());
        assertTrue(service.handle(new SignInCommand("a b", "secret123")).isFailure());
    }

    @Test
    void createUserUsesDefaultPasswordAndGivenRoles() {
        Result<User, UserCommandFailure> result = service.handle(new CreateUserCommand(
                "mrojas", "maria.rojas@andina.pe", null, "María Rojas", List.of("ROLE_AUDITOR"), 1L, 2L));

        User user = result.success().orElseThrow();
        assertEquals("hashed:12345678", user.getPasswordHash());
        assertEquals(List.of("ROLE_AUDITOR"), user.getRoleNames());
        assertEquals(2L, user.getWarehouseId());
    }

    @Test
    void updateUserChangesRolesAndRejectsTakenEmail() {
        service.handle(new SignUpCommand("jquispe", "juan.quispe@andina.pe", "secret123", "Juan Quispe", 1L));
        service.handle(new SignUpCommand("mrojas", "maria.rojas@andina.pe", "secret123", "María Rojas", 1L));

        Result<User, UserCommandFailure> updated = service.handle(
                new UpdateUserCommand(1L, null, null, List.of("ROLE_SECURITY_MANAGER"), 3L));
        assertEquals(List.of("ROLE_SECURITY_MANAGER"), updated.success().orElseThrow().getRoleNames());

        Result<User, UserCommandFailure> taken = service.handle(
                new UpdateUserCommand(1L, "maria.rojas@andina.pe", null, null, null));
        assertTrue(taken.failure().orElseThrow() instanceof UserCommandFailure.EmailTaken);
    }

    @Test
    void deleteUserFailsWhenUserDoesNotExist() {
        Result<Long, UserCommandFailure> result = service.handle(new DeleteUserCommand(99L));

        assertTrue(result.failure().orElseThrow() instanceof UserCommandFailure.NotFound);
    }

    @Test
    void deleteUserRemovesIt() {
        service.handle(new SignUpCommand("jquispe", "juan.quispe@andina.pe", "secret123", "Juan Quispe", 1L));

        assertTrue(service.handle(new DeleteUserCommand(1L)).isSuccess());
        assertFalse(repository.existsById(1L));
    }

    private static class FakeHasher implements PasswordHasher {
        @Override
        public String hash(String password) {
            return "hashed:" + password;
        }

        @Override
        public boolean matches(String password, String hash) {
            return hash.equals("hashed:" + password);
        }
    }

    private static class FakeTokenService implements TokenService {
        @Override
        public String createToken(User user) {
            return "token-for-" + user.getUsername().value();
        }

        @Override
        public Optional<SessionUser> readToken(String token) {
            return Optional.empty();
        }
    }

    private static class FakeUserRepository implements UserRepository {
        private final Map<Long, User> users = new LinkedHashMap<>();
        private long nextId = 1;

        @Override
        public User save(User user) {
            Long id = user.getId() != null ? user.getId() : nextId++;
            User saved = User.restore(id, user.getUsername(), user.getEmail(), user.getFullName(),
                    user.getPasswordHash(), user.getRoles(), user.getCompanyId(), user.getWarehouseId(),
                    user.getCreatedAt(), user.getUpdatedAt());
            users.put(id, saved);
            return saved;
        }

        @Override
        public Optional<User> findById(Long id) {
            return Optional.ofNullable(users.get(id));
        }

        @Override
        public Optional<User> findByUsername(Username username) {
            return users.values().stream().filter(user -> user.getUsername().equals(username)).findFirst();
        }

        @Override
        public Optional<User> findByEmail(EmailAddress email) {
            return users.values().stream().filter(user -> user.getEmail().equals(email)).findFirst();
        }

        @Override
        public List<User> findAll() {
            return List.copyOf(users.values());
        }

        @Override
        public boolean existsById(Long id) {
            return users.containsKey(id);
        }

        @Override
        public boolean existsByUsername(Username username) {
            return findByUsername(username).isPresent();
        }

        @Override
        public boolean existsByEmail(EmailAddress email) {
            return findByEmail(email).isPresent();
        }

        @Override
        public void deleteById(Long id) {
            users.remove(id);
        }

        @Override
        public long count() {
            return users.size();
        }
    }
}
