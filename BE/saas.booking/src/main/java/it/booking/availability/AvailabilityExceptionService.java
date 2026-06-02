package it.booking.availability;

import static it.booking.common.TextUtils.trimNullable;

import it.booking.booking.BookingRepository;
import it.booking.booking.BookingStatus;
import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import it.booking.offering.OfferedService;
import it.booking.offering.OfferedServiceRepository;
import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AvailabilityExceptionService {

    private final AvailabilityExceptionRepository exceptions;
    private final BookingRepository bookings;
    private final ProviderRepository providers;
    private final OfferedServiceRepository offeredServices;
    private final AvailabilityExceptionMapper mapper;

    public AvailabilityExceptionService(
            AvailabilityExceptionRepository exceptions,
            BookingRepository bookings,
            ProviderRepository providers,
            OfferedServiceRepository offeredServices,
            AvailabilityExceptionMapper mapper
    ) {
        this.exceptions = exceptions;
        this.bookings = bookings;
        this.providers = providers;
        this.offeredServices = offeredServices;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<AvailabilityExceptionResponse> listForCurrentProvider(Long userId) {
        Provider provider = providerForUser(userId);
        return exceptions.findAllByProviderIdOrderByStartsAtAsc(provider.getId())
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AvailabilityExceptionResponse getForCurrentProvider(Long userId, Long exceptionId) {
        Provider provider = providerForUser(userId);
        return exceptions.findByIdAndProviderId(exceptionId, provider.getId())
                .map(mapper::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.AVAILABILITY_EXCEPTION_NOT_FOUND));
    }

    @Transactional
    public AvailabilityExceptionResponse createForCurrentProvider(Long userId, CreateAvailabilityExceptionRequest request) {
        Provider provider = providerForUserForUpdate(userId);
        OfferedService service = serviceForProvider(request.serviceId(), provider.getId());
        validateInterval(request.startsAt(), request.endsAt());
        validateNoOverlap(provider.getId(), request.serviceId(), null, request.startsAt(), request.endsAt());
        validateNoActiveBooking(provider.getId(), request.serviceId(), request.startsAt(), request.endsAt());

        AvailabilityException exception = exceptions.save(new AvailabilityException(
                provider,
                service,
                request.startsAt(),
                request.endsAt(),
                trimNullable(request.reason())
        ));
        return mapper.toResponse(exception);
    }

    @Transactional
    public AvailabilityExceptionResponse updateForCurrentProvider(
            Long userId,
            Long exceptionId,
            UpdateAvailabilityExceptionRequest request
    ) {
        Provider provider = providerForUserForUpdate(userId);
        AvailabilityException exception = exceptions.findByIdAndProviderId(exceptionId, provider.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.AVAILABILITY_EXCEPTION_NOT_FOUND));
        OfferedService service = serviceForProvider(request.serviceId(), provider.getId());
        validateInterval(request.startsAt(), request.endsAt());

        if (request.active()) {
            validateNoOverlap(provider.getId(), request.serviceId(), exceptionId, request.startsAt(), request.endsAt());
            validateNoActiveBooking(provider.getId(), request.serviceId(), request.startsAt(), request.endsAt());
        }

        exception.update(
                service,
                request.startsAt(),
                request.endsAt(),
                trimNullable(request.reason()),
                request.active()
        );
        return mapper.toResponse(exception);
    }

    @Transactional
    public void activateForCurrentProvider(Long userId, Long exceptionId) {
        Provider provider = providerForUserForUpdate(userId);
        AvailabilityException exception = exceptions.findByIdAndProviderId(exceptionId, provider.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.AVAILABILITY_EXCEPTION_NOT_FOUND));
        Long serviceId = exception.getService() == null ? null : exception.getService().getId();
        validateNoOverlap(provider.getId(), serviceId, exceptionId, exception.getStartsAt(), exception.getEndsAt());
        validateNoActiveBooking(provider.getId(), serviceId, exception.getStartsAt(), exception.getEndsAt());
        exception.activate();
    }

    @Transactional
    public void deactivateForCurrentProvider(Long userId, Long exceptionId) {
        Provider provider = providerForUser(userId);
        AvailabilityException exception = exceptions.findByIdAndProviderId(exceptionId, provider.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.AVAILABILITY_EXCEPTION_NOT_FOUND));
        exception.deactivate();
    }

    @Transactional
    public void deleteForCurrentProvider(Long userId, Long exceptionId) {
        deactivateForCurrentProvider(userId, exceptionId);
    }

    private Provider providerForUser(Long userId) {
        return providers.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
    }

    private Provider providerForUserForUpdate(Long userId) {
        return providers.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
    }

    private OfferedService serviceForProvider(Long serviceId, Long providerId) {
        if (serviceId == null) {
            return null;
        }
        return offeredServices.findByIdAndProviderId(serviceId, providerId)
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_FOUND));
    }

    private void validateInterval(Instant startsAt, Instant endsAt) {
        if (!endsAt.isAfter(startsAt)) {
            throw new ApiException(ErrorCode.INVALID_AVAILABILITY_EXCEPTION_INTERVAL);
        }
    }

    private void validateNoOverlap(Long providerId, Long serviceId, Long exceptionId, Instant startsAt, Instant endsAt) {
        if (exceptions.existsActiveOverlap(providerId, serviceId, exceptionId, startsAt, endsAt)) {
            throw new ApiException(ErrorCode.AVAILABILITY_EXCEPTION_OVERLAP);
        }
    }

    private void validateNoActiveBooking(Long providerId, Long serviceId, Instant startsAt, Instant endsAt) {
        boolean exists = serviceId == null
                ? bookings.existsByProviderIdAndStatusInAndStartsAtLessThanAndEndsAtGreaterThan(
                        providerId,
                        BookingStatus.blockingStatuses(),
                        endsAt,
                        startsAt
                )
                : bookings.existsByProviderIdAndServiceIdAndStatusInAndStartsAtLessThanAndEndsAtGreaterThan(
                        providerId,
                        serviceId,
                        BookingStatus.blockingStatuses(),
                        endsAt,
                        startsAt
                );
        if (exists) {
            throw new ApiException(ErrorCode.AVAILABILITY_EXCEPTION_BOOKING_CONFLICT);
        }
    }
}
