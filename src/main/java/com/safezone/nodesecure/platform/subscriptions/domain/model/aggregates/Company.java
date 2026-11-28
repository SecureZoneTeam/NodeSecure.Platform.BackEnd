package com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates;

import com.safezone.nodesecure.platform.subscriptions.domain.model.valueobjects.TaxId;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * Legal (billing) data of an organization: one company per organization and a RUC that is
 * unique across the whole platform. {@code companyId} is the organization (tenant) id.
 */
@Entity
@Table(name = "companies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_companies_tax_id", columnNames = "tax_id"),
        @UniqueConstraint(name = "uk_companies_company_id", columnNames = "company_id")})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "company_id", nullable = false, updatable = false)
    private Long companyId;

    @Column(nullable = false, length = 150)
    private String legalName;

    @Column(name = "tax_id", nullable = false, length = 11, updatable = false)
    private String taxId;

    @Column(nullable = false, length = 120)
    private String contactEmail;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public static Company register(Long companyId, String legalName, TaxId taxId, String contactEmail,
                                   String country, Instant now) {
        Company company = new Company();
        company.companyId = Objects.requireNonNull(companyId);
        company.legalName = requireText(legalName, 150, "companies.error.legalName.invalid");
        company.taxId = Objects.requireNonNull(taxId).value();
        company.contactEmail = requireText(contactEmail, 120, "companies.error.contactEmail.invalid")
                .toLowerCase(Locale.ROOT);
        String countryCode = country == null || country.isBlank() ? "PE" : country.trim().toUpperCase(Locale.ROOT);
        if (!countryCode.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("companies.error.country.invalid");
        }
        company.country = countryCode;
        company.createdAt = now;
        company.updatedAt = now;
        return company;
    }

    private static String requireText(String value, int max, String errorKey) {
        if (value == null || value.isBlank() || value.trim().length() > max) {
            throw new IllegalArgumentException(errorKey);
        }
        return value.trim();
    }
}
