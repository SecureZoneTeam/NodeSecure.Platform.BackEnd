package com.safezone.nodesecure.platform.subscriptions.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.subscriptions.application.internal.services.InvoiceService;
import com.safezone.nodesecure.platform.subscriptions.interfaces.rest.resources.SubscriptionResources.InvoiceResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

/** T28: invoices of my organization. They are issued only by subscription activations and plan changes. */
@RestController
@RequestMapping(value = "/api/v1/invoices", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Invoices", description = "Invoices of my organization (read only)")
public class InvoicesController {

    private final InvoiceService invoiceService;
    private final IamContextFacade iamFacade;

    public InvoicesController(InvoiceService invoiceService, IamContextFacade iamFacade) {
        this.invoiceService = invoiceService;
        this.iamFacade = iamFacade;
    }

    @Operation(summary = "Invoices of my organization, newest first; optional filter by subscription")
    @PreAuthorize("hasAuthority('subscriptions.view')")
    @GetMapping
    public ResponseEntity<List<InvoiceResource>> getAll(@RequestParam(required = false) Long subscriptionId) {
        return ResponseEntity.ok(invoiceService.findAll(iamFacade.requireCompanyId(), subscriptionId)
                .stream().map(InvoiceResource::from).toList());
    }

    @Operation(summary = "Get an invoice of my organization")
    @ApiResponse(responseCode = "404", description = "Not found in my organization")
    @PreAuthorize("hasAuthority('subscriptions.view')")
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResource> getById(@PathVariable Long id) {
        return ResponseEntity.ok(InvoiceResource.from(invoiceService.getById(id, iamFacade.requireCompanyId())));
    }
}
