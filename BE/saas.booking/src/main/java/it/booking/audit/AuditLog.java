package it.booking.audit;

import it.booking.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private AppUser actor;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditEventType eventType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditEntityType entityType;

    private Long entityId;

    @Column(columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected AuditLog() {
    }

    public AuditLog(AppUser actor, AuditEventType eventType, AuditEntityType entityType, Long entityId, String payload) {
        this.actor = actor;
        this.eventType = eventType;
        this.entityType = entityType;
        this.entityId = entityId;
        this.payload = payload;
    }

    public Long getId() {
        return id;
    }

    public AppUser getActor() {
        return actor;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public AuditEntityType getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
