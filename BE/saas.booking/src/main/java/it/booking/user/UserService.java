package it.booking.user;

import static it.booking.common.EmailUtils.normalize;

import it.booking.audit.AuditService;
import it.booking.booking.Booking;
import it.booking.booking.BookingRepository;
import it.booking.booking.BookingStatus;
import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import it.booking.config.CacheInvalidator;
import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final String ACCOUNT_DISABLED_BOOKING_CANCELLATION_REASON = "Account customer disabilitato da admin";
    private static final String PROVIDER_ACCOUNT_DISABLED_BOOKING_CANCELLATION_REASON = "Account provider disabilitato da admin";

    private final AppUserRepository users;
    private final ProviderRepository providers;
    private final BookingRepository bookings;
    private final AuditService auditService;
    private final CacheInvalidator cacheInvalidator;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(
            AppUserRepository users,
            ProviderRepository providers,
            BookingRepository bookings,
            AuditService auditService,
            CacheInvalidator cacheInvalidator,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper
    ) {
        this.users = users;
        this.providers = providers;
        this.bookings = bookings;
        this.auditService = auditService;
        this.cacheInvalidator = cacheInvalidator;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return users.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (request.role() == UserRole.PROVIDER) {
            throw new ApiException(ErrorCode.USER_PROVIDER_ROLE_REQUIRES_PROFILE);
        }

        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        AppUser user = users.save(new AppUser(email, passwordEncoder.encode(request.password()), request.role()));
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request, Long adminId) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        AppUser admin = users.findById(adminId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() != UserRole.PROVIDER && request.role() == UserRole.PROVIDER) {
            throw new ApiException(ErrorCode.USER_PROVIDER_ROLE_REQUIRES_PROFILE);
        }
        if (user.getRole() == UserRole.PROVIDER && request.role() != UserRole.PROVIDER) {
            throw new ApiException(ErrorCode.USER_PROVIDER_ROLE_CHANGE_NOT_ALLOWED);
        }

        String email = normalize(request.email());
        if (users.existsByEmailAndIdNot(email, id)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        boolean shouldDisableUser = user.isEnabled() && !request.enabled();
        boolean shouldCancelCustomerBookings = shouldDisableUser && user.getRole() == UserRole.CUSTOMER;
        boolean shouldCancelProviderBookings = shouldDisableUser && user.getRole() == UserRole.PROVIDER;
        user.update(email, request.role(), request.enabled());
        if (shouldCancelCustomerBookings) {
            cancelActiveCustomerBookings(user, admin);
        }
        if (shouldCancelProviderBookings) {
            deactivateProviderAndCancelBookings(user, admin);
        }
        return userMapper.toResponse(user);
    }

    @Transactional
    public void enable(Long id) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        user.enable();
    }

    @Transactional
    public void disable(Long id, Long adminId) {
        AppUser user = users.findById(id)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        AppUser admin = users.findById(adminId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        boolean shouldCancelCustomerBookings = user.isEnabled() && user.getRole() == UserRole.CUSTOMER;
        boolean shouldCancelProviderBookings = user.isEnabled() && user.getRole() == UserRole.PROVIDER;
        user.disable();
        if (shouldCancelCustomerBookings) {
            cancelActiveCustomerBookings(user, admin);
        }
        if (shouldCancelProviderBookings) {
            deactivateProviderAndCancelBookings(user, admin);
        }
    }

    @Transactional
    public void delete(Long id, Long adminId) {
        disable(id, adminId);
    }

    private void cancelActiveCustomerBookings(AppUser customer, AppUser admin) {
        List<Booking> activeBookings = bookings.findAllByCustomerIdAndStatusInAndEndsAtGreaterThanOrderByStartsAtAsc(
                customer.getId(),
                BookingStatus.blockingStatuses(),
                Instant.now()
        );
        activeBookings.forEach(booking -> {
            booking.cancel(admin, ACCOUNT_DISABLED_BOOKING_CANCELLATION_REASON);
            auditService.bookingCancelled(admin, booking, ACCOUNT_DISABLED_BOOKING_CANCELLATION_REASON);
        });
    }

    private void deactivateProviderAndCancelBookings(AppUser providerUser, AppUser admin) {
        providers.findByUserId(providerUser.getId()).ifPresent(provider -> {
            boolean wasActive = provider.isActive();
            provider.deactivate();
            cacheInvalidator.providerVisibilityChanged(provider.getId());
            if (wasActive) {
                cancelActiveProviderBookings(provider, admin);
            }
        });
    }

    private void cancelActiveProviderBookings(Provider provider, AppUser admin) {
        List<Booking> activeBookings = bookings.findAllByProviderIdAndStatusInAndEndsAtGreaterThanOrderByStartsAtAsc(
                provider.getId(),
                BookingStatus.blockingStatuses(),
                Instant.now()
        );
        activeBookings.forEach(booking -> {
            booking.cancel(admin, PROVIDER_ACCOUNT_DISABLED_BOOKING_CANCELLATION_REASON);
            auditService.bookingCancelled(admin, booking, PROVIDER_ACCOUNT_DISABLED_BOOKING_CANCELLATION_REASON);
        });
    }
}
