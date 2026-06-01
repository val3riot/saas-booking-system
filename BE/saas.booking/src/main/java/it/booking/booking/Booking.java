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

    protected Booking() {
    }

    public Booking(AppUser customer, Provider provider, OfferedService service, Instant startsAt, Instant endsAt) {
        this.customer = customer;
        this.provider = provider;
        this.service = service;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = BookingStatus.PENDING;
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

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void update(Provider provider, OfferedService service, Instant startsAt, Instant endsAt) {
        this.provider = provider;
        this.service = service;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.updatedAt = Instant.now();
    }
}
