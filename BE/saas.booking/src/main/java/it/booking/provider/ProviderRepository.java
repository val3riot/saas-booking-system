package it.booking.provider;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderRepository extends JpaRepository<Provider, Long>, JpaSpecificationExecutor<Provider> {

    List<Provider> findAllByOrderByBusinessNameAsc();

    List<Provider> findAllByActiveTrueOrderByBusinessNameAsc();

    Optional<Provider> findByUserId(Long userId);

    Optional<Provider> findByIdAndActiveTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select provider from Provider provider where provider.id = :id")
    Optional<Provider> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select provider from Provider provider where provider.user.id = :userId")
    Optional<Provider> findByUserIdForUpdate(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByIdAndActiveTrue(Long id);

    boolean existsByUserIdAndIdNot(Long userId, Long id);
}
