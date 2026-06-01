package it.booking.auth;

import static it.booking.common.EmailUtils.normalize;
import static it.booking.common.TextUtils.trim;
import static it.booking.common.TextUtils.trimNullable;

import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import it.booking.user.AppUser;
import it.booking.user.AppUserRepository;
import it.booking.user.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AppUserRepository users;
    private final ProviderRepository providers;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AppUserRepository users,
            ProviderRepository providers,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.users = users;
        this.providers = providers;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        AppUser user = users.save(new AppUser(email, passwordEncoder.encode(request.password()), UserRole.CUSTOMER));
        return AuthResponse.bearer(jwtService.issueToken(user.getId(), user.getEmail(), user.getRole()));
    }

    @Transactional
    public AuthResponse registerProvider(ProviderRegistrationRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        AppUser user = users.save(new AppUser(email, passwordEncoder.encode(request.password()), UserRole.PROVIDER));
        providers.save(new Provider(
                user,
                trim(request.businessName()),
                trimNullable(request.description()),
                trim(request.category()),
                trim(request.city()),
                trimNullable(request.address())
        ));

        return AuthResponse.bearer(jwtService.issueToken(user.getId(), user.getEmail(), user.getRole()));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        String email = normalize(request.email());
        AppUser user = users.findByEmail(email)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!user.isEnabled()) {
            throw new ApiException(ErrorCode.ACCOUNT_DISABLED);
        }

        return AuthResponse.bearer(jwtService.issueToken(user.getId(), user.getEmail(), user.getRole()));
    }
}
