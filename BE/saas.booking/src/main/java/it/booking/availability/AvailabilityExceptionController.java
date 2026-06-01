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
@RequestMapping("/api/providers/me/availability-exceptions")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class AvailabilityExceptionController {

    private final AvailabilityExceptionService availabilityExceptionService;

    public AvailabilityExceptionController(AvailabilityExceptionService availabilityExceptionService) {
        this.availabilityExceptionService = availabilityExceptionService;
    }

    @GetMapping
    List<AvailabilityExceptionResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return availabilityExceptionService.listForCurrentProvider(user.id());
    }

    @GetMapping("/{exceptionId}")
    AvailabilityExceptionResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long exceptionId) {
        return availabilityExceptionService.getForCurrentProvider(user.id(), exceptionId);
    }

    @PostMapping
    ResponseEntity<AvailabilityExceptionResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateAvailabilityExceptionRequest request
    ) {
        AvailabilityExceptionResponse response = availabilityExceptionService.createForCurrentProvider(user.id(), request);
        return ResponseEntity.created(URI.create("/api/providers/me/availability-exceptions/" + response.id())).body(response);
    }

    @PutMapping("/{exceptionId}")
    AvailabilityExceptionResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long exceptionId,
            @Valid @RequestBody UpdateAvailabilityExceptionRequest request
    ) {
        return availabilityExceptionService.updateForCurrentProvider(user.id(), exceptionId, request);
    }

    @PostMapping("/{exceptionId}/activate")
    @ApiResponse(responseCode = "204", description = "Availability exception activated")
    ResponseEntity<Void> activate(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long exceptionId) {
        availabilityExceptionService.activateForCurrentProvider(user.id(), exceptionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{exceptionId}/deactivate")
    @ApiResponse(responseCode = "204", description = "Availability exception deactivated")
    ResponseEntity<Void> deactivate(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long exceptionId) {
        availabilityExceptionService.deactivateForCurrentProvider(user.id(), exceptionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{exceptionId}")
    @ApiResponse(responseCode = "204", description = "Availability exception deactivated")
    ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long exceptionId) {
        availabilityExceptionService.deleteForCurrentProvider(user.id(), exceptionId);
        return ResponseEntity.noContent().build();
    }
}
