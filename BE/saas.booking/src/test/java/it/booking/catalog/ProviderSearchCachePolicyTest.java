package it.booking.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class ProviderSearchCachePolicyTest {

    private final ProviderSearchCachePolicy policy = new ProviderSearchCachePolicy();

    @Test
    void cachesFrontendInitialSearch() {
        assertThat(policy.isBaseSearch(
                "",
                null,
                " ",
                null,
                0,
                10,
                CatalogProviderSort.BUSINESS_NAME,
                Sort.Direction.ASC
        )).isTrue();
    }

    @Test
    void doesNotCacheFilteredSearches() {
        assertThat(policy.isBaseSearch(
                "studio",
                null,
                null,
                null,
                0,
                10,
                CatalogProviderSort.BUSINESS_NAME,
                Sort.Direction.ASC
        )).isFalse();

        assertThat(policy.isBaseSearch(
                null,
                null,
                null,
                LocalDate.of(2026, 6, 8),
                0,
                10,
                CatalogProviderSort.BUSINESS_NAME,
                Sort.Direction.ASC
        )).isFalse();
    }

    @Test
    void doesNotCacheDifferentPaginationOrSorting() {
        assertThat(policy.isBaseSearch(
                null,
                null,
                null,
                null,
                1,
                10,
                CatalogProviderSort.BUSINESS_NAME,
                Sort.Direction.ASC
        )).isFalse();

        assertThat(policy.isBaseSearch(
                null,
                null,
                null,
                null,
                0,
                20,
                CatalogProviderSort.CITY,
                Sort.Direction.DESC
        )).isFalse();
    }
}
