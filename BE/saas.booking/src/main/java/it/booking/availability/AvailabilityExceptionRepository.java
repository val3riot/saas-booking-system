package it.booking.availability;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityExceptionRepository extends JpaRepository<AvailabilityException, Long> {

    List<AvailabilityException> findAllByProviderIdOrderByStartsAtAsc(Long providerId);

    Optional<AvailabilityException> findByIdAndProviderId(Long id, Long providerId);

    @Query("""
            select exception
            from AvailabilityException exception
            where exception.provider.id = :providerId
              and exception.active = true
              and (exception.service is null or exception.service.id = :serviceId)
              and exception.startsAt < :endsAt
              and exception.endsAt > :startsAt
            order by exception.startsAt
            """)
    List<AvailabilityException> findActiveBlocking(
            @Param("providerId") Long providerId,
            @Param("serviceId") Long serviceId,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt
    );

    @Query("""
            select count(exception) > 0
            from AvailabilityException exception
            where exception.provider.id = :providerId
              and exception.active = true
              and (exception.service is null or exception.service.id = :serviceId)
              and exception.startsAt < :endsAt
              and exception.endsAt > :startsAt
            """)
    boolean existsActiveBlocking(
            @Param("providerId") Long providerId,
            @Param("serviceId") Long serviceId,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt
    );

    @Query("""
            select count(exception) > 0
            from AvailabilityException exception
            where exception.provider.id = :providerId
              and exception.active = true
              and (:id is null or exception.id <> :id)
              and (
                    exception.service is null
                    or :serviceId is null
                    or exception.service.id = :serviceId
              )
              and exception.startsAt < :endsAt
              and exception.endsAt > :startsAt
            """)
    boolean existsActiveOverlap(
            @Param("providerId") Long providerId,
            @Param("serviceId") Long serviceId,
            @Param("id") Long id,
            @Param("startsAt") Instant startsAt,
            @Param("endsAt") Instant endsAt
    );
}
