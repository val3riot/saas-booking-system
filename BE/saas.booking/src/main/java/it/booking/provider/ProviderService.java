package it.booking.provider;

import static it.booking.common.EmailUtils.normalize;
import static it.booking.common.TextUtils.trim;
import static it.booking.common.TextUtils.trimNullable;

import it.booking.audit.AuditService;
import it.booking.booking.Booking;
import it.booking.booking.BookingRepository;
import it.booking.booking.BookingStatus;
import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import it.booking.user.AppUser;
import it.booking.user.AppUserRepository;
import it.booking.user.UserRole;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderService {

    private static final String PROVIDER_DEACTIVATED_BOOKING_CANCELLATION_REASON = "Provider disattivato da admin";

    private final ProviderRepository providers;
    private final AppUserRepository users;
    private final BookingRepository bookings;
    private final AuditService auditService;
    private final ProviderMapper providerMapper;
    private final PasswordEncoder passwordEncoder;

    public ProviderService(
            ProviderRepository providers,
            AppUserRepository users,
            BookingRepository bookings,
            AuditService auditService,
            ProviderMapper providerMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.providers = providers;
        this.users = users;
        this.bookings = bookings;
        this.auditService = auditService;
        this.providerMapper = providerMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<ProviderResponse> list() {
        return providers.findAllByOrderByBusinessNameAsc()
                .stream()
                .map(providerMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProviderResponse get(Long id) {
        return providers.findById(id)
                .map(providerMapper::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public ProviderResponse getByUserId(Long userId) {
        return getProviderByUserId(userId)
                .map(providerMapper::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
    }

    @Transactional
    public ProviderResponse createByUserId(Long userId, CreateProviderProfileRequest request) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        validateProviderUser(user);

        if (providers.existsByUserId(userId)) {
            throw new ApiException(ErrorCode.PROVIDER_ALREADY_EXISTS);
        }

        Provider provider = providers.save(new Provider(
                user,
                trim(request.businessName()),
                trimNullable(request.description()),
                trim(request.category()),
                trim(request.city()),
                trimNullable(request.address())
        ));
        return providerMapper.toResponse(provider);
    }

    @Transactional
    public ProviderResponse create(CreateProviderRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        AppUser user = users.save(new AppUser(email, passwordEncoder.encode(request.password()), UserRole.PROVIDER));
        Provider provider = providers.save(new Provider(
                user,
                trim(request.businessName()),
                trimNullable(request.description()),
                trim(request.category()),
                trim(request.city()),
                trimNullable(request.address())
        ));
        return providerMapper.toResponse(provider);
    }

    @Transactional
    public ProviderResponse update(Long id, UpdateProviderRequest request, Long adminId) {
        Provider provider = providers.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
        AppUser admin = users.findById(adminId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        AppUser user = users.findById(request.userId())
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        validateProviderUser(user);

        if (providers.existsByUserIdAndIdNot(request.userId(), id)) {
            throw new ApiException(ErrorCode.PROVIDER_ALREADY_EXISTS);
        }

        if (!provider.getUser().getId().equals(user.getId())) {
            provider.changeUser(user);
        }

        boolean shouldCancelProviderBookings = provider.isActive() && !request.active();
        provider.update(
                trim(request.businessName()),
                trimNullable(request.description()),
                trim(request.category()),
                trim(request.city()),
                trimNullable(request.address()),
                request.active()
        );
        if (shouldCancelProviderBookings) {
            cancelActiveProviderBookings(provider, admin, PROVIDER_DEACTIVATED_BOOKING_CANCELLATION_REASON);
        }
        return providerMapper.toResponse(provider);
    }

    @Transactional
    public ProviderResponse updateByUserId(Long userId, UpdateProviderProfileRequest request) {
        Provider provider = getProviderByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));

        provider.update(
                trim(request.businessName()),
                trimNullable(request.description()),
                trim(request.category()),
                trim(request.city()),
                trimNullable(request.address()),
                provider.isActive()
        );
        return providerMapper.toResponse(provider);
    }

    @Transactional
    public void activate(Long id) {
        Provider provider = providers.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
        if (!provider.getUser().isEnabled()) {
            throw new ApiException(ErrorCode.PROVIDER_USER_DISABLED);
        }
        provider.activate();
    }

    @Transactional
    public void deactivate(Long id, Long adminId) {
        Provider provider = providers.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
        AppUser admin = users.findById(adminId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        boolean shouldCancelProviderBookings = provider.isActive();
        provider.deactivate();
        if (shouldCancelProviderBookings) {
            cancelActiveProviderBookings(provider, admin, PROVIDER_DEACTIVATED_BOOKING_CANCELLATION_REASON);
        }
    }

    @Transactional
    public void delete(Long id, Long adminId) {
        deactivate(id, adminId);
    }

    private void validateProviderUser(AppUser user) {
        if (user.getRole() != UserRole.PROVIDER) {
            throw new ApiException(ErrorCode.PROVIDER_USER_ROLE_REQUIRED);
        }
    }

    private Optional<Provider> getProviderByUserId(Long userId) {
        return providers.findByUserId(userId);
    }

    private void cancelActiveProviderBookings(Provider provider, AppUser actor, String reason) {
        List<Booking> activeBookings = bookings.findAllByProviderIdAndStatusInAndEndsAtGreaterThanOrderByStartsAtAsc(
                provider.getId(),
                BookingStatus.blockingStatuses(),
                Instant.now()
        );
        activeBookings.forEach(booking -> {
            booking.cancel(actor, reason);
            auditService.bookingCancelled(actor, booking, reason);
        });
    }
}
