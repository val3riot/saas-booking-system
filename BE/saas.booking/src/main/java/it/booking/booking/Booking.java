package it.booking.booking;

import it.booking.offering.OfferedService;
import it.booking.provider.Provider;
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
import it.booking.common.ApiException;
import it.booking.common.ErrorCode;
import java.time.Instant;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private AppUser customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private OfferedService service;

    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.CONFIRMED;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    private Instant cancelledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id")
    private AppUser cancelledBy;

    private String cancellationReason;

    protected Booking() {
    }

    public Booking(AppUser customer, Provider provider, OfferedService service, Instant startsAt, Instant endsAt) {
        this.customer = customer;
        this.provider = provider;
        this.service = service;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = BookingStatus.CONFIRMED;
    }

    public Long getId() {
        return id;
    }

    public AppUser getCustomer() {
        return customer;
    }

    public Provider getProvider() {
        return provider;
    }

    public OfferedService getService() {
        return service;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public AppUser getCancelledBy() {
        return cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void cancel(AppUser cancelledBy, String reason) {
        if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) {
            throw new ApiException(ErrorCode.BOOKING_STATUS_TRANSITION_NOT_ALLOWED);
        }
        Instant now = Instant.now();
        this.status = BookingStatus.CANCELLED;
        this.cancelledAt = now;
        this.cancelledBy = cancelledBy;
        this.cancellationReason = reason;
        this.updatedAt = now;
    }
}
