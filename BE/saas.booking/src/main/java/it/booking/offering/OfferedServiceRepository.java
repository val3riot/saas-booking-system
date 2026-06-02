package it.booking.offering;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferedServiceRepository extends JpaRepository<OfferedService, Long> {

    List<OfferedService> findAllByProviderIdOrderByNameAsc(Long providerId);

    List<OfferedService> findAllByProviderIdAndActiveTrueOrderByNameAsc(Long providerId);

    Optional<OfferedService> findByIdAndProviderId(Long id, Long providerId);

    Optional<OfferedService> findByIdAndProviderIdAndActiveTrue(Long id, Long providerId);

    boolean existsByProviderIdAndNameIgnoreCase(Long providerId, String name);

    boolean existsByProviderIdAndNameIgnoreCaseAndIdNot(Long providerId, String name, Long id);
}
