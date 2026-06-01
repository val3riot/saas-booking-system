package it.booking.offering;

import org.springframework.stereotype.Component;

@Component
public class OfferedServiceMapper {

    public OfferedServiceResponse toResponse(OfferedService service) {
        return new OfferedServiceResponse(
                service.getId(),
                service.getProvider().getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.getPriceCents(),
                service.isActive(),
                service.getCreatedAt(),
                service.getUpdatedAt()
        );
    }
}
