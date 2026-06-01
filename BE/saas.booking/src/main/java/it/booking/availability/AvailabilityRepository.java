package it.booking.availability;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    List<Availability> findAllByProviderIdAndServiceIdOrderByDayOfWeekAscStartTimeAsc(Long providerId, Long serviceId);

    List<Availability> findAllByProviderIdAndServiceIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(Long providerId, Long serviceId);

    Optional<Availability> findByIdAndProviderIdAndServiceId(Long id, Long providerId, Long serviceId);

    @Query("""
            select count(availability) > 0
            from Availability availability
            where availability.provider.id = :providerId
              and availability.service.id = :serviceId
              and availability.dayOfWeek = :dayOfWeek
              and availability.active = true
              and availability.startTime < :endTime
              and availability.endTime > :startTime
            """)
    boolean existsActiveOverlap(
            @Param("providerId") Long providerId,
            @Param("serviceId") Long serviceId,
            @Param("dayOfWeek") short dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("""
            select count(availability) > 0
            from Availability availability
            where availability.provider.id = :providerId
              and availability.service.id = :serviceId
              and availability.dayOfWeek = :dayOfWeek
              and availability.active = true
              and availability.id <> :id
              and availability.startTime < :endTime
              and availability.endTime > :startTime
            """)
    boolean existsActiveOverlapExcludingId(
            @Param("providerId") Long providerId,
            @Param("serviceId") Long serviceId,
            @Param("id") Long id,
            @Param("dayOfWeek") short dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );

    @Query("""
            select count(availability) > 0
            from Availability availability
            where availability.provider.id = :providerId
              and availability.service.id = :serviceId
              and availability.dayOfWeek = :dayOfWeek
              and availability.active = true
              and availability.startTime <= :startTime
              and availability.endTime >= :endTime
            """)
    boolean existsActiveCovering(
            @Param("providerId") Long providerId,
            @Param("serviceId") Long serviceId,
            @Param("dayOfWeek") short dayOfWeek,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime
    );
}
