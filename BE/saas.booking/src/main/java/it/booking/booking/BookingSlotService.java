package it.booking.booking;

import it.booking.availability.Availability;
import it.booking.availability.AvailabilityRepository;
import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import it.booking.offering.OfferedService;
import it.booking.offering.OfferedServiceRepository;
import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingSlotService {

    private static final List<BookingStatus> BLOCKING_STATUSES = List.of(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED
    );

    private final ProviderRepository providers;
    private final OfferedServiceRepository offeredServices;
    private final AvailabilityRepository availabilities;
    private final BookingRepository bookings;

    public BookingSlotService(
            ProviderRepository providers,
            OfferedServiceRepository offeredServices,
            AvailabilityRepository availabilities,
            BookingRepository bookings
    ) {
        this.providers = providers;
        this.offeredServices = offeredServices;
        this.availabilities = availabilities;
        this.bookings = bookings;
    }

    @Transactional(readOnly = true)
    public List<BookingSlotResponse> listSlots(Long providerId, Long serviceId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new ApiException(ErrorCode.INVALID_FIELD_VALUE);
        }

        Provider provider = providers.findById(providerId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));
        OfferedService service = offeredServices.findByIdAndProviderId(serviceId, provider.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.SERVICE_NOT_FOUND));
        validateProviderAndService(provider, service);

        List<Availability> rules = availabilities.findAllByProviderIdAndServiceIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
                providerId,
                serviceId
        );
        Instant rangeStart = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant rangeEnd = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Booking> bookedSlots = bookings
                .findAllByProviderIdAndStatusInAndStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(
                        providerId,
                        BLOCKING_STATUSES,
                        rangeEnd,
                        rangeStart
                );

        List<BookingSlotResponse> slots = new ArrayList<>();
        Instant now = Instant.now();
        LocalDate current = from;
        while (!current.isAfter(to)) {
            short dayOfWeek = (short) current.getDayOfWeek().getValue();
            for (Availability rule : rules) {
                if (rule.getDayOfWeek() == dayOfWeek) {
                    addSlotsForRule(slots, service, current, rule, bookedSlots, now);
                }
            }
            current = current.plusDays(1);
        }
        return slots;
    }

    private void addSlotsForRule(
            List<BookingSlotResponse> slots,
            OfferedService service,
            LocalDate date,
            Availability rule,
            List<Booking> bookedSlots,
            Instant now
    ) {
        LocalTime startTime = rule.getStartTime();
        LocalTime endTime = startTime.plusMinutes(service.getDurationMinutes());
        while (!endTime.isAfter(rule.getEndTime())) {
            Instant startsAt = date.atTime(startTime).toInstant(ZoneOffset.UTC);
            Instant endsAt = date.atTime(endTime).toInstant(ZoneOffset.UTC);
            if (startsAt.isAfter(now)) {
                slots.add(new BookingSlotResponse(
                        rule.getProvider().getId(),
                        service.getId(),
                        date,
                        startsAt,
                        endsAt,
                        isBooked(bookedSlots, startsAt, endsAt) ? BookingSlotStatus.BOOKED : BookingSlotStatus.AVAILABLE
                ));
            }
            startTime = endTime;
            endTime = startTime.plusMinutes(service.getDurationMinutes());
        }
    }

    private boolean isBooked(List<Booking> bookedSlots, Instant startsAt, Instant endsAt) {
        return bookedSlots.stream()
                .anyMatch(booking -> booking.getStartsAt().isBefore(endsAt) && booking.getEndsAt().isAfter(startsAt));
    }

    private void validateProviderAndService(Provider provider, OfferedService service) {
        if (!provider.isActive()) {
            throw new ApiException(ErrorCode.PROVIDER_NOT_AVAILABLE);
        }
        if (!service.isActive()) {
            throw new ApiException(ErrorCode.SERVICE_NOT_AVAILABLE);
        }
    }
}
