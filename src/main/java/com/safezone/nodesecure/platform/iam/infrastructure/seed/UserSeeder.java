package com.safezone.nodesecure.platform.iam.infrastructure.seed;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Role;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import com.safezone.nodesecure.platform.iam.domain.services.PasswordHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;

@Slf4j
@Component
public class UserSeeder implements CommandLineRunner {

    private final UserRepository repository;
    private final PasswordHasher hasher;
    private final Clock clock;
    private final String defaultPassword;

    public UserSeeder(UserRepository repository, PasswordHasher hasher, Clock clock,
                      @Value("${iam.default-password}") String defaultPassword) {
        this.repository = repository;
        this.hasher = hasher;
        this.clock = clock;
        this.defaultPassword = defaultPassword;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        String hash = hasher.hash(defaultPassword);
        Instant now = clock.instant();

        save("fperez", "fernando.perez@safezone.pe", "Fernando Sebastián Pérez Bellido", hash, Role.ROLE_ADMINISTRATOR, 4L, now);
        save("lanahua", "liz.anahua@safezone.pe", "Liz Maribel Anahua Ancachi", hash, Role.ROLE_SECURITY_MANAGER, 3L, now);
        save("ksandoval", "kelber.sandoval@safezone.pe", "Kelber Yamir Sandoval Aiquipa", hash, Role.ROLE_WAREHOUSE_KEEPER, 2L, now);
        save("lravello", "luciana.ravello@safezone.pe", "Luciana Angielina Ravello Cárdenas", hash, Role.ROLE_AUDITOR, 1L, now);

        log.info("Demo users created");
    }

    private void save(String username, String email, String fullName, String hash, Role role, Long warehouseId, Instant now) {
        User user = User.create(new Username(username), new EmailAddress(email), fullName, hash,
                Set.of(role), 1L, warehouseId, now);
        repository.save(user);
    }
}
