package it.booking.offering;

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
@RequestMapping("/api/providers/me/services")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class OfferedServiceController {

    private final OfferedServiceService offeredServiceService;

    public OfferedServiceController(OfferedServiceService offeredServiceService) {
        this.offeredServiceService = offeredServiceService;
    }

    @GetMapping
    List<OfferedServiceResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return offeredServiceService.listForCurrentProvider(user.id());
    }

    @GetMapping("/{serviceId}")
    OfferedServiceResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long serviceId) {
        return offeredServiceService.getForCurrentProvider(user.id(), serviceId);
    }

    @PostMapping
    ResponseEntity<OfferedServiceResponse> create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateOfferedServiceRequest request
    ) {
        OfferedServiceResponse response = offeredServiceService.createForCurrentProvider(user.id(), request);
        return ResponseEntity.created(URI.create("/api/providers/me/services/" + response.id())).body(response);
    }

    @PutMapping("/{serviceId}")
    OfferedServiceResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long serviceId,
            @Valid @RequestBody UpdateOfferedServiceRequest request
    ) {
        return offeredServiceService.updateForCurrentProvider(user.id(), serviceId, request);
    }

    @PostMapping("/{serviceId}/activate")
    @ApiResponse(responseCode = "204", description = "Service activated")
    ResponseEntity<Void> activate(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long serviceId) {
        offeredServiceService.activateForCurrentProvider(user.id(), serviceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{serviceId}/deactivate")
    @ApiResponse(responseCode = "204", description = "Service deactivated")
    ResponseEntity<Void> deactivate(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long serviceId) {
        offeredServiceService.deactivateForCurrentProvider(user.id(), serviceId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{serviceId}")
    @ApiResponse(responseCode = "204", description = "Service deactivated")
    ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long serviceId) {
        offeredServiceService.deleteForCurrentProvider(user.id(), serviceId);
        return ResponseEntity.noContent().build();
    }
}
