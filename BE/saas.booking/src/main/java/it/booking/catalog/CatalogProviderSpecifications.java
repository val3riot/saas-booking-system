package it.booking.catalog;

import it.booking.availability.Availability;
import it.booking.offering.OfferedService;
import it.booking.provider.Provider;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

final class CatalogProviderSpecifications {

    private CatalogProviderSpecifications() {
    }

    static Specification<Provider> search(
            String query,
            String category,
            String city,
            Short dayOfWeek
    ) {
        return (provider, criteriaQuery, criteriaBuilder) -> {
            criteriaQuery.distinct(true);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(provider.get("active")));

            if (query != null) {
                String searchPattern = likePattern(query);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(provider.get("businessName")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(provider.get("description"), "")), searchPattern)
                ));
            }

            if (category != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(provider.get("category")),
                        category.toLowerCase(Locale.ROOT)
                ));
            }

            if (city != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(provider.get("city")),
                        city.toLowerCase(Locale.ROOT)
                ));
            }

            if (dayOfWeek != null) {
                predicates.add(hasActiveAvailabilityOnDay(provider, dayOfWeek, criteriaQuery.subquery(Long.class), criteriaBuilder));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate hasActiveAvailabilityOnDay(
            Root<Provider> provider,
            Short dayOfWeek,
            Subquery<Long> subquery,
            CriteriaBuilder criteriaBuilder
    ) {
        Root<Availability> availability = subquery.from(Availability.class);
        Join<Availability, OfferedService> service = availability.join("service");

        subquery.select(availability.get("id"))
                .where(
                        criteriaBuilder.equal(availability.get("provider"), provider),
                        criteriaBuilder.isTrue(availability.get("active")),
                        criteriaBuilder.isTrue(service.get("active")),
                        criteriaBuilder.equal(availability.get("dayOfWeek"), dayOfWeek)
                );

        return criteriaBuilder.exists(subquery);
    }

    private static String likePattern(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }
}
