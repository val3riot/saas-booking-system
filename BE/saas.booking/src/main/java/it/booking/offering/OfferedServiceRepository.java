package it.booking.offering;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferedServiceRepository extends JpaRepository<OfferedService, Long> {

    Optional<OfferedService> findByIdAndProviderId(Long id, Long providerId);
}
