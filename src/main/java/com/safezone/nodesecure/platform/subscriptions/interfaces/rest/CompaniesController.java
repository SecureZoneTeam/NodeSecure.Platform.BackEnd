package com.safezone.nodesecure.platform.subscriptions.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.subscriptions.application.internal.services.CompanyService;
import com.safezone.nodesecure.platform.subscriptions.application.internal.services.SubscriptionService;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Company;
import com.safezone.nodesecure.platform.subscriptions.domain.model.aggregates.Subscription;
import com.safezone.nodesecure.platform.subscriptions.interfaces.rest.resources.SubscriptionResources.CompanyResource;
import com.safezone.nodesecure.platform.subscriptions.interfaces.rest.resources.SubscriptionResources.CreateCompanyResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/** T25: legal data (RUC) of the signed-in owner's organization. Never exposes other organizations. */
@RestController
@RequestMapping(value = "/api/v1/companies", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Companies", description = "Legal data (RUC) of my organization")
public class CompaniesController {

    private final CompanyService companyService;
    private final SubscriptionService subscriptionService;
    private final IamContextFacade iamFacade;

    public CompaniesController(CompanyService companyService, SubscriptionService subscriptionService,
                               IamContextFacade iamFacade) {
        this.companyService = companyService;
        this.subscriptionService = subscriptionService;
        this.iamFacade = iamFacade;
    }

    @Operation(summary = "Register the company of my organization",
            description = "RUC must have 11 digits, a valid prefix and SUNAT check digit, and be unique in the platform.")
    @ApiResponse(responseCode = "201", description = "Company registered")
    @ApiResponse(responseCode = "400", description = "Invalid data or RUC")
    @ApiResponse(responseCode = "409", description = "RUC already registered or organization already has a company")
    @PreAuthorize("hasAuthority('settings.manage')")
    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<CompanyResource> register(@Valid @RequestBody CreateCompanyResource resource) {
        Long companyId = iamFacade.requireCompanyId();
        Company company = companyService.register(companyId, new CompanyService.RegisterCompany(
                resource.legalName(), resource.taxId(), resource.contactEmail(), resource.country()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResource(company));
    }

    @Operation(summary = "Company of my organization (empty list when not registered); optional search by RUC")
    @PreAuthorize("hasAuthority('settings.view')")
    @GetMapping
    public ResponseEntity<List<CompanyResource>> getAll(
            @Parameter(description = "RUC to search for") @RequestParam(required = false) String taxId) {
        Long companyId = iamFacade.requireCompanyId();
        Optional<Company> company = taxId == null || taxId.isBlank()
                ? companyService.findMine(companyId)
                : companyService.findByTaxId(taxId, companyId);
        return ResponseEntity.ok(company.map(this::toResource).stream().toList());
    }

    @Operation(summary = "Get the company of my organization by id")
    @ApiResponse(responseCode = "404", description = "Not found in my organization")
    @PreAuthorize("hasAuthority('settings.view')")
    @GetMapping("/{id}")
    public ResponseEntity<CompanyResource> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toResource(companyService.getById(id, iamFacade.requireCompanyId())));
    }

    private CompanyResource toResource(Company company) {
        Long subscriptionId = subscriptionService.findActive(company.getCompanyId()).map(Subscription::getId).orElse(null);
        return CompanyResource.from(company, subscriptionId);
    }
}
