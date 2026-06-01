package it.booking.booking;

import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getCustomer().getId(),
                booking.getProvider().getId(),
                booking.getProvider().getBusinessName(),
                booking.getService().getId(),
                booking.getService().getName(),
                booking.getStartsAt(),
                booking.getEndsAt(),
                booking.getStatus(),
                booking.getCancelledAt(),
                booking.getCancelledBy() == null ? null : booking.getCancelledBy().getId(),
                booking.getCancellationReason(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}
