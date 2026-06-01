package it.booking.availability;

import org.springframework.stereotype.Component;

@Component
public class AvailabilityMapper {

    public AvailabilityResponse toResponse(Availability availability) {
        return new AvailabilityResponse(
                availability.getId(),
                availability.getProvider().getId(),
                availability.getService().getId(),
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.isActive(),
                availability.getCreatedAt(),
                availability.getUpdatedAt()
        );
    }
}
