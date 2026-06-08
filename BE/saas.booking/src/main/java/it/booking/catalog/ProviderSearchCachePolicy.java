package it.booking.catalog;

import java.time.LocalDate;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class ProviderSearchCachePolicy {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;

    public boolean isBaseSearch(
            String query,
            String category,
            String city,
            LocalDate availableOn,
            int page,
            int size,
            CatalogProviderSort sort,
            Sort.Direction direction
    ) {
        return isBlank(query)
                && isBlank(category)
                && isBlank(city)
                && availableOn == null
                && page == DEFAULT_PAGE
                && size == DEFAULT_SIZE
                && sort == CatalogProviderSort.BUSINESS_NAME
                && direction == Sort.Direction.ASC;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
