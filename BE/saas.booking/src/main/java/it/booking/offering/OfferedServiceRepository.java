package it.booking.offering;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferedServiceRepository extends JpaRepository<OfferedService, Long> {

    List<OfferedService> findAllByProviderIdOrderByNameAsc(Long providerId);

    List<OfferedService> findAllByProviderIdAndActiveTrueOrderByNameAsc(Long providerId);

    Optional<OfferedService> findByIdAndProviderId(Long id, Long providerId);

    Optional<OfferedService> findByIdAndProviderIdAndActiveTrue(Long id, Long providerId);

    boolean existsByProviderIdAndNameIgnoreCase(Long providerId, String name);

    @Query("""
            select count(service) > 0
            from OfferedService service
            where service.provider.id = :providerId
              and lower(service.name) = lower(:name)
              and service.id <> :id
            """)
    boolean existsByProviderIdAndNameIgnoreCaseAndIdNot(
            @Param("providerId") Long providerId,
            @Param("name") String name,
            @Param("id") Long id
    );
}
