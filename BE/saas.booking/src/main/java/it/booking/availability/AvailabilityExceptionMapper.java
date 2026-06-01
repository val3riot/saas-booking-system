package it.booking.availability;

import org.springframework.stereotype.Component;

@Component
class AvailabilityExceptionMapper {

    AvailabilityExceptionResponse toResponse(AvailabilityException exception) {
        return new AvailabilityExceptionResponse(
                exception.getId(),
                exception.getProvider().getId(),
                exception.getService() == null ? null : exception.getService().getId(),
                exception.getStartsAt(),
                exception.getEndsAt(),
                exception.getReason(),
                exception.isActive(),
                exception.getCreatedAt(),
                exception.getUpdatedAt()
        );
    }
}
