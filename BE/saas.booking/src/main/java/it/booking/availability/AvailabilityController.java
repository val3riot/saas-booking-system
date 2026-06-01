package it.booking.availability;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.booking.auth.AuthenticatedUser;
import it.booking.config.OpenApiConfig;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers/me/services/{serviceId}/availabilities")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    List<AvailabilityResponse> list(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long serviceId) {
        return availabilityService.listForCurrentProvider(user.id(), serviceId);
    }

    @GetMapping("/{availabilityId}")
    AvailabilityResponse get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long serviceId,
            @PathVariable Long availabilityId
    ) {
        return availabilityService.getForCurrentProvider(user.id(), serviceId, availabilityId);
    }

    @PostMapping
    ResponseEntity<AvailabilityResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long serviceId,
            @Valid @RequestBody CreateAvailabilityRequest request
    ) {
        AvailabilityResponse response = availabilityService.createForCurrentProvider(user.id(), serviceId, request);
        return ResponseEntity.created(URI.create("/api/providers/me/services/" + serviceId + "/availabilities/" + response.id())).body(response);
    }

    @PutMapping("/{availabilityId}")
    AvailabilityResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long serviceId,
            @PathVariable Long availabilityId,
            @Valid @RequestBody UpdateAvailabilityRequest request
    ) {
        return availabilityService.updateForCurrentProvider(user.id(), serviceId, availabilityId, request);
    }

    @PostMapping("/{availabilityId}/activate")
    @ApiResponse(responseCode = "204", description = "Availability activated")
    ResponseEntity<Void> activate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long serviceId,
            @PathVariable Long availabilityId
    ) {
        availabilityService.activateForCurrentProvider(user.id(), serviceId, availabilityId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{availabilityId}/deactivate")
    @ApiResponse(responseCode = "204", description = "Availability deactivated")
    ResponseEntity<Void> deactivate(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long serviceId,
            @PathVariable Long availabilityId
    ) {
        availabilityService.deactivateForCurrentProvider(user.id(), serviceId, availabilityId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{availabilityId}")
    @ApiResponse(responseCode = "204", description = "Availability deactivated")
    ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long serviceId,
            @PathVariable Long availabilityId
    ) {
        availabilityService.deleteForCurrentProvider(user.id(), serviceId, availabilityId);
        return ResponseEntity.noContent().build();
    }
}
