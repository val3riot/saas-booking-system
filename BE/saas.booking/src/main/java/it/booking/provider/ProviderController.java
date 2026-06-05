package it.booking.provider;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping("/api/providers")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @GetMapping("/me")
    ProviderResponse me(@AuthenticationPrincipal AuthenticatedUser user) {
        return providerService.getByUserId(user.id());
    }

    @PostMapping("/me")
    ResponseEntity<ProviderResponse> createMe(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateProviderProfileRequest request
    ) {
        ProviderResponse response = providerService.createByUserId(user.id(), request);
        return ResponseEntity.created(URI.create("/api/providers/me")).body(response);
    }

    @PutMapping("/me")
    ProviderResponse updateMe(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody UpdateProviderProfileRequest request
    ) {
        return providerService.updateByUserId(user.id(), request);
    }

    @GetMapping
    List<ProviderResponse> list() {
        return providerService.list();
    }

    @GetMapping("/{id}")
    ProviderResponse get(@PathVariable Long id) {
        return providerService.get(id);
    }

    @PostMapping
    ResponseEntity<ProviderResponse> create(@Valid @RequestBody CreateProviderRequest request) {
        ProviderResponse response = providerService.create(request);
        return ResponseEntity.created(URI.create("/api/providers/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    ProviderResponse update(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable Long id,
            @Valid @RequestBody UpdateProviderRequest request
    ) {
        return providerService.update(id, request, admin.id());
    }

    @PostMapping("/{id}/activate")
    @ApiResponse(responseCode = "204", description = "Provider activated")
    ResponseEntity<Void> activate(@PathVariable Long id) {
        providerService.activate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deactivate")
    @ApiResponse(responseCode = "204", description = "Provider deactivated")
    ResponseEntity<Void> deactivate(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        providerService.deactivate(id, admin.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "Provider deactivated")
    ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        providerService.delete(id, admin.id());
        return ResponseEntity.noContent().build();
    }
}
