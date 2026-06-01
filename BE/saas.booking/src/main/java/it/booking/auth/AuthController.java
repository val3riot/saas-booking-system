package it.booking.auth;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    AuthResponse register(@Valid @RequestBody AuthRequest request) {
        return authService.register(request);
    }

    @PostMapping("/register/provider")
    AuthResponse registerProvider(@Valid @RequestBody ProviderRegistrationRequest request) {
        return authService.registerProvider(request);
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody AuthRequest request) {
        return authService.login(request);
    }
}
