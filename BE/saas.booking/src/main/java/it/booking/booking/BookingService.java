package it.booking.booking;

import it.booking.availability.Availability;
import it.booking.availability.AvailabilityRepository;
import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import it.booking.offering.OfferedService;
import it.booking.offering.OfferedServiceRepository;
import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import it.booking.user.AppUser;
import it.booking.user.AppUserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final List<BookingStatus> BLOCKING_STATUSES = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED
    );

    private final BookingRepository bookings;
    private final AppUserRepository users;
    private final ProviderRepository providers;
    private final OfferedServiceRepository offeredServices;
    private final AvailabilityRepository availabilities;
    private final BookingMapper bookingMapper;

    public BookingService(
            BookingRepository bookings,
            AppUserRepository users,
            ProviderRepository providers,
            OfferedServiceRepository offeredServices,
            AvailabilityRepository availabilities,
            BookingMapper bookingMapper
    ) {
        this.bookings = bookings;
        this.users = users;
        this.providers = providers;
        this.offeredServices = offeredServices;
        this.availabilities = availabilities;
        this.bookingMapper = bookingMapper;
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listForCurrentCustomer(Long customerId) {
        return bookings.findAllByCustomerIdOrderByStartsAtAsc(customerId)
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getForCurrentCustomer(Long customerId, Long bookingId) {
        return bookings.findByIdAndCustomerId(bookingId, customerId)
                .map(bookingMapper::toResponse)
                .orElseThrow(() -> new ApiException(ErrorCode.BOOKING_NOT_FOUND));
    }

    @Transactional
    public BookingResponse createForCurrentCustomer(Long customerId, CreateBookingRequest request) {
        AppUser customer = users.findById(customerId)
                .orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        Provider provider = providers.findByIdForUpdate(request.providerId())
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
        OfferedService service = offeredServices.findByIdAndProviderId(request.serviceId(), provider.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_FOUND));

        validateProviderAndService(provider, service);

        Instant startsAt = request.startsAt();
        Instant endsAt = startsAt.plusSeconds((long) service.getDurationMinutes() * 60);
        validateAvailability(provider.getId(), service.getId(), service.getDurationMinutes(), startsAt, endsAt);
        validateNoBookingOverlap(provider.getId(), startsAt, endsAt);

        Booking booking = bookings.save(new Booking(customer, provider, service, startsAt, endsAt));
        return bookingMapper.toResponse(booking);
    }

    @Transactional
    public void cancelForCurrentCustomer(Long customerId, Long bookingId) {
        Booking booking = bookings.findByIdAndCustomerId(bookingId, customerId)
                .orElseThrow(() -> new ApiException(ErrorCode.BOOKING_NOT_FOUND));
        booking.cancel();
    }

    private void validateProviderAndService(Provider provider, OfferedService service) {
        if (!provider.isActive()) {
            throw new ApiException(ErrorCode.PROVIDER_NOT_AVAILABLE);
        }
        if (!service.isActive()) {
            throw new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE);
        }
    }

    private void validateAvailability(Long providerId, Long serviceId, int durationMinutes, Instant startsAt, Instant endsAt) {
        LocalDate startDate = startsAt.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = endsAt.atZone(ZoneOffset.UTC).toLocalDate();
        if (!startDate.equals(endDate)) {
            throw new ApiException(ErrorCode.BOOKING_SLOT_UNAVAILABLE);
        }

        short dayOfWeek = (short) startsAt.atZone(ZoneOffset.UTC).getDayOfWeek().getValue();
        LocalTime startTime = startsAt.atZone(ZoneOffset.UTC).toLocalTime();
        LocalTime endTime = endsAt.atZone(ZoneOffset.UTC).toLocalTime();
        boolean validGeneratedSlot = availabilities.findAllByProviderIdAndServiceIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(providerId, serviceId)
                .stream()
                .filter(availability -> availability.getDayOfWeek() == dayOfWeek)
                .anyMatch(availability -> coversGeneratedSlot(availability, durationMinutes, startTime, endTime));

        if (!validGeneratedSlot) {
            throw new ApiException(ErrorCode.BOOKING_SLOT_UNAVAILABLE);
        }
    }

    private boolean coversGeneratedSlot(
            Availability availability,
            int durationMinutes,
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (startTime.isBefore(availability.getStartTime()) || endTime.isAfter(availability.getEndTime())) {
            return false;
        }
        long minutesFromAvailabilityStart = java.time.Duration.between(availability.getStartTime(), startTime).toMinutes();
        return minutesFromAvailabilityStart % durationMinutes == 0;
    }

    private void validateNoBookingOverlap(Long providerId, Instant startsAt, Instant endsAt) {
        if (bookings.existsByProviderIdAndStatusInAndStartsAtLessThanAndEndsAtGreaterThan(
                providerId,
                BLOCKING_STATUSES,
                endsAt,
                startsAt
        )) {
            throw new ApiException(ErrorCode.BOOKING_SLOT_UNAVAILABLE);
        }
    }
}
