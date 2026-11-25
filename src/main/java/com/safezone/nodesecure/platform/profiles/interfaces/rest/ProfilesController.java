package com.safezone.nodesecure.platform.profiles.interfaces.rest;

import com.safezone.nodesecure.platform.iam.interfaces.acl.IamContextFacade;
import com.safezone.nodesecure.platform.profiles.domain.model.aggregates.Profile;
import com.safezone.nodesecure.platform.profiles.domain.repositories.ProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Profiles of the signed-in user's organization (team administration, owner-only). */
@RestController
@RequestMapping("/api/v1/profiles")
@Tag(name = "Profiles", description = "Profiles of my organization")
public class ProfilesController {

    private final ProfileRepository profileRepository;
    private final IamContextFacade iamFacade;

    public ProfilesController(ProfileRepository profileRepository, IamContextFacade iamFacade) {
        this.profileRepository = profileRepository;
        this.iamFacade = iamFacade;
    }

    @Operation(summary = "Profiles of my organization")
    @PreAuthorize("hasAuthority('users.view')")
    @GetMapping
    public ResponseEntity<List<ProfileResource>> getAllProfiles() {
        return ResponseEntity.ok(profileRepository.findAllByCompanyIdOrderByIdAsc(iamFacade.requireCompanyId())
                .stream().map(ProfileResource::from).toList());
    }

    @Operation(summary = "Create a profile in my organization")
    @PreAuthorize("hasAuthority('users.create')")
    @PostMapping
    public ResponseEntity<ProfileResource> createProfile(@Valid @RequestBody CreateProfileResource resource) {
        Profile profile = new Profile(resource.firstName().trim(), resource.lastName().trim(),
                resource.email().trim().toLowerCase(), resource.role().trim());
        profile.setCompanyId(iamFacade.requireCompanyId());
        Profile savedProfile = profileRepository.save(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProfileResource.from(savedProfile));
    }

    @Operation(summary = "Get a profile of my organization")
    @PreAuthorize("hasAuthority('users.view')")
    @GetMapping("/{id}")
    public ResponseEntity<ProfileResource> getProfileById(@PathVariable Long id) {
        return profileRepository.findByIdAndCompanyId(id, iamFacade.requireCompanyId())
                .map(profile -> ResponseEntity.ok(ProfileResource.from(profile)))
                .orElse(ResponseEntity.notFound().build());
    }

    public record CreateProfileResource(@NotBlank String firstName, @NotBlank String lastName,
                                        @NotBlank @Email String email, @NotBlank String role) {
    }

    public record ProfileResource(Long id, Long companyId, String firstName, String lastName, String email,
                                  String role) {
        static ProfileResource from(Profile profile) {
            return new ProfileResource(profile.getId(), profile.getCompanyId(), profile.getFirstName(),
                    profile.getLastName(), profile.getEmail(), profile.getRole());
        }
    }
}
