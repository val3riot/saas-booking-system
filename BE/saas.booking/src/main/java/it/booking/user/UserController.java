package it.booking.user;

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
@RequestMapping("/api/users")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    List<UserResponse> list() {
        return userService.list();
    }

    @GetMapping("/{id}")
    UserResponse get(@PathVariable Long id) {
        return userService.get(id);
    }

    @PostMapping
    ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.created(URI.create("/api/users/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    UserResponse update(
            @AuthenticationPrincipal AuthenticatedUser admin,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.update(id, request, admin.id());
    }

    @PostMapping("/{id}/enable")
    @ApiResponse(responseCode = "204", description = "User enabled")
    ResponseEntity<Void> enable(@PathVariable Long id) {
        userService.enable(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/disable")
    @ApiResponse(responseCode = "204", description = "User disabled")
    ResponseEntity<Void> disable(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        userService.disable(id, admin.id());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @ApiResponse(responseCode = "204", description = "User disabled")
    ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser admin, @PathVariable Long id) {
        userService.delete(id, admin.id());
        return ResponseEntity.noContent().build();
    }
}
