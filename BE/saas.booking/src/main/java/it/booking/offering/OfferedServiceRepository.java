package it.booking.offering;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OfferedServiceRepository extends JpaRepository<OfferedService, Long> {

    List<OfferedService> findAllByProviderIdOrderByNameAsc(Long providerId);

    List<OfferedService> findAllByProviderIdAndActiveTrueOrderByNameAsc(Long providerId);

    @Query("""
            select distinct service
            from OfferedService service
            join Availability availability on availability.service = service
            where service.provider.id = :providerId
              and service.active = true
              and service.provider.active = true
              and service.provider.user.enabled = true
              and availability.active = true
            order by service.name asc
            """)
    List<OfferedService> findAllBookableByProviderIdOrderByNameAsc(@Param("providerId") Long providerId);

    Optional<OfferedService> findByIdAndProviderId(Long id, Long providerId);

    Optional<OfferedService> findByIdAndProviderIdAndActiveTrue(Long id, Long providerId);

    @Query("""
            select service
            from OfferedService service
            where service.id = :id
              and service.provider.id = :providerId
              and service.active = true
              and service.provider.active = true
              and service.provider.user.enabled = true
              and exists (
                  select availability.id
                  from Availability availability
                  where availability.service = service
                    and availability.active = true
              )
            """)
    Optional<OfferedService> findBookableByIdAndProviderId(@Param("id") Long id, @Param("providerId") Long providerId);

    boolean existsByProviderIdAndNameIgnoreCase(Long providerId, String name);

    boolean existsByProviderIdAndNameIgnoreCaseAndIdNot(Long providerId, String name, Long id);
}
