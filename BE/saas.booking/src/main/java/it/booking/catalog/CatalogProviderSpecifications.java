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
            predicates.add(criteriaBuilder.isTrue(provider.get("user").get("enabled")));

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

            predicates.add(hasBookableService(provider, dayOfWeek, criteriaQuery.subquery(Long.class), criteriaBuilder));

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate hasBookableService(
            Root<Provider> provider,
            Short dayOfWeek,
            Subquery<Long> subquery,
            CriteriaBuilder criteriaBuilder
    ) {
        Root<Availability> availability = subquery.from(Availability.class);
        Join<Availability, OfferedService> service = availability.join("service");

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(availability.get("provider"), provider));
        predicates.add(criteriaBuilder.isTrue(availability.get("active")));
        predicates.add(criteriaBuilder.isTrue(service.get("active")));

        if (dayOfWeek != null) {
            predicates.add(criteriaBuilder.equal(availability.get("dayOfWeek"), dayOfWeek));
        }

        subquery.select(availability.get("id"))
                .where(criteriaBuilder.and(predicates.toArray(Predicate[]::new)));

        return criteriaBuilder.exists(subquery);
    }

    private static String likePattern(String value) {
        return "%" + value.toLowerCase(Locale.ROOT) + "%";
    }
}
