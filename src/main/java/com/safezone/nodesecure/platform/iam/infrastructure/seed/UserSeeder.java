package com.safezone.nodesecure.platform.iam.infrastructure.seed;

import com.safezone.nodesecure.platform.iam.domain.model.aggregates.Organization;
import com.safezone.nodesecure.platform.iam.domain.model.aggregates.User;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.EmailAddress;
import com.safezone.nodesecure.platform.iam.domain.model.valueobjects.Username;
import com.safezone.nodesecure.platform.iam.domain.repositories.OrganizationRepository;
import com.safezone.nodesecure.platform.iam.domain.repositories.UserRepository;
import com.safezone.nodesecure.platform.iam.domain.services.PasswordHasher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Creates a demo organization with its owner when the users table is empty (fresh local database).
 * It never runs on a database that already has users, and can be turned off with IAM_SEED_DEMO_DATA=false.
 * Workers are not seeded: the owner creates them from "Equipos y accesos" after creating warehouses.
 */
@Slf4j
@Component
public class UserSeeder implements CommandLineRunner {

    private final UserRepository repository;
    private final OrganizationRepository organizationRepository;
    private final PasswordHasher hasher;
    private final Clock clock;
    private final String defaultPassword;
    private final boolean enabled;

    public UserSeeder(UserRepository repository, OrganizationRepository organizationRepository, PasswordHasher hasher,
                      Clock clock, @Value("${iam.default-password}") String defaultPassword,
                      @Value("${iam.seed-demo-data:true}") boolean enabled) {
        this.repository = repository;
        this.organizationRepository = organizationRepository;
        this.hasher = hasher;
        this.clock = clock;
        this.defaultPassword = defaultPassword;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || repository.count() > 0) {
            return;
        }
        Instant now = clock.instant();
        Organization organization = organizationRepository.save(Organization.create("NodeSecure Demo", now));
        User owner = repository.save(User.registerOwner(new Username("fperez"),
                new EmailAddress("fernando.perez@safezone.pe"), "Fernando Sebastián Pérez Bellido",
                hasher.hash(defaultPassword), organization.getId(), now));
        organization.assignOwner(owner.getId(), now);
        organizationRepository.save(organization);
        log.info("Demo organization created with owner fperez");
    }
}
