package it.booking.booking;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByCustomerIdOrderByStartsAtAsc(Long customerId);

    Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);

    List<Booking> findAllByCustomerIdAndStatusInAndEndsAtGreaterThanOrderByStartsAtAsc(
            Long customerId,
            List<BookingStatus> statuses,
            java.time.Instant now
    );

    List<Booking> findAllByProviderIdOrderByStartsAtAsc(Long providerId);

    Optional<Booking> findByIdAndProviderId(Long id, Long providerId);

    List<Booking> findAllByProviderIdAndStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(
            Long providerId,
            java.time.Instant endsAt,
            java.time.Instant startsAt
    );

    List<Booking> findAllByProviderIdAndStatusInAndStartsAtLessThanAndEndsAtGreaterThanOrderByStartsAtAsc(
            Long providerId,
            List<BookingStatus> statuses,
            java.time.Instant endsAt,
            java.time.Instant startsAt
    );

    List<Booking> findAllByProviderIdAndStatusInAndEndsAtGreaterThanOrderByStartsAtAsc(
            Long providerId,
            List<BookingStatus> statuses,
            java.time.Instant now
    );

    boolean existsByProviderIdAndStatusInAndStartsAtLessThanAndEndsAtGreaterThan(
            Long providerId,
            List<BookingStatus> statuses,
            java.time.Instant endsAt,
            java.time.Instant startsAt
    );

    boolean existsByProviderIdAndServiceIdAndStatusInAndStartsAtLessThanAndEndsAtGreaterThan(
            Long providerId,
            Long serviceId,
            List<BookingStatus> statuses,
            java.time.Instant endsAt,
            java.time.Instant startsAt
    );

    boolean existsByProviderIdAndStatusInAndStartsAtLessThanAndEndsAtGreaterThanAndIdNot(
            Long providerId,
            List<BookingStatus> statuses,
            java.time.Instant endsAt,
            java.time.Instant startsAt,
            Long id
    );
}
