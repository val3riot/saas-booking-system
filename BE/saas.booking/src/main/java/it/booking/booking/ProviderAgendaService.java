package it.booking.booking;

import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderAgendaService {

    private final ProviderRepository providers;
    private final BookingRepository bookings;
    private final BookingMapper bookingMapper;

    public ProviderAgendaService(
            ProviderRepository providers,
            BookingRepository bookings,
            BookingMapper bookingMapper
    ) {
        this.providers = providers;
        this.bookings = bookings;
        this.bookingMapper = bookingMapper;
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listForCurrentProvider(Long userId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new ApiException(ErrorCode.INVALID_FIELD_VALUE);
        }

        Provider provider = providers.findByUserId(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.PROVIDER_NOT_FOUND));

        return bookings.findAllByProviderIdAndStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(
                        provider.getId(),
                        to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                        from.atStartOfDay().toInstant(ZoneOffset.UTC)
                )
                .stream()
                .map(bookingMapper::toResponse)
                .toList();
    }
}
