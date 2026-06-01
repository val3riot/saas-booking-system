package it.booking.booking;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByCustomerIdOrderByStartsAtAsc(Long customerId);

    Optional<Booking> findByIdAndCustomerId(Long id, Long customerId);

    boolean existsByProviderIdAndStatusInAndStartsAtLessThanAndEndsAtGreaterThan(
            Long providerId,
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
