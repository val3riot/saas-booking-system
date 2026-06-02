package it.booking.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.booking.booking.Booking;
import it.booking.user.AppUser;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogs;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogs, ObjectMapper objectMapper) {
        this.auditLogs = auditLogs;
        this.objectMapper = objectMapper;
    }

    public void bookingCreated(AppUser actor, Booking booking) {
        save(actor, AuditEventType.BOOKING_CREATED, booking, Map.of(
                "status", booking.getStatus(),
                "customerId", booking.getCustomer().getId(),
                "providerId", booking.getProvider().getId(),
                "serviceId", booking.getService().getId(),
                "startsAt", booking.getStartsAt(),
                "endsAt", booking.getEndsAt()
        ));
    }

    public void bookingConfirmed(AppUser actor, Booking booking) {
        save(actor, AuditEventType.BOOKING_CONFIRMED, booking, Map.of("status", booking.getStatus()));
    }

    public void bookingRejected(AppUser actor, Booking booking, String reason) {
        save(actor, AuditEventType.BOOKING_REJECTED, booking, payloadWithOptionalReason(booking, reason));
    }

    public void bookingCancelled(AppUser actor, Booking booking, String reason) {
        save(actor, AuditEventType.BOOKING_CANCELLED, booking, payloadWithOptionalReason(booking, reason));
    }

    private Map<String, Object> payloadWithOptionalReason(Booking booking, String reason) {
        if (reason == null || reason.isBlank()) {
            return Map.of("status", booking.getStatus());
        }
        return Map.of("status", booking.getStatus(), "reason", reason);
    }

    private void save(AppUser actor, AuditEventType eventType, Booking booking, Map<String, Object> payload) {
        auditLogs.save(new AuditLog(
                actor,
                eventType,
                AuditEntityType.BOOKING,
                booking.getId(),
                serialize(payload)
        ));
    }

    private String serialize(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize audit payload", exception);
        }
    }
}
