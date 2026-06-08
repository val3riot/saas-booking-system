package it.booking.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import it.booking.availability.Availability;
import it.booking.availability.AvailabilityRepository;
import it.booking.common.PageResponse;
import it.booking.offering.OfferedService;
import it.booking.offering.OfferedServiceRepository;
import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import it.booking.user.AppUser;
import it.booking.user.AppUserRepository;
import it.booking.user.UserRole;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = "spring.cache.type=simple")
class ProviderSearchCachingIntegrationTest {

    @Autowired
    private CatalogService catalogService;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private ProviderRepository providers;

    @Autowired
    private OfferedServiceRepository offeredServices;

    @Autowired
    private AvailabilityRepository availabilities;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void cachesOnlyFrontendInitialSearch() {
        Provider provider = createBookableProvider("base-cache@example.com", "Base Cache Studio");

        PageResponse<CatalogProviderResponse> firstBaseSearch = baseSearch();
        provider.deactivate();
        providers.saveAndFlush(provider);
        PageResponse<CatalogProviderResponse> secondBaseSearch = baseSearch();

        assertThat(firstBaseSearch.content()).extracting(CatalogProviderResponse::id).contains(provider.getId());
        assertThat(secondBaseSearch).isEqualTo(firstBaseSearch);

        Provider filteredProvider = createBookableProvider("filtered-cache@example.com", "Filtered Cache Studio");
        PageResponse<CatalogProviderResponse> firstFilteredSearch = filteredSearch("filtered cache");
        filteredProvider.deactivate();
        providers.saveAndFlush(filteredProvider);
        PageResponse<CatalogProviderResponse> secondFilteredSearch = filteredSearch("filtered cache");

        assertThat(firstFilteredSearch.content()).extracting(CatalogProviderResponse::id).contains(filteredProvider.getId());
        assertThat(secondFilteredSearch.content()).extracting(CatalogProviderResponse::id).doesNotContain(filteredProvider.getId());
    }

    private PageResponse<CatalogProviderResponse> baseSearch() {
        return catalogService.searchProviders(
                null,
                null,
                null,
                null,
                0,
                10,
                CatalogProviderSort.BUSINESS_NAME,
                Sort.Direction.ASC
        );
    }

    private PageResponse<CatalogProviderResponse> filteredSearch(String query) {
        return catalogService.searchProviders(
                query,
                null,
                null,
                null,
                0,
                10,
                CatalogProviderSort.BUSINESS_NAME,
                Sort.Direction.ASC
        );
    }

    private Provider createBookableProvider(String email, String businessName) {
        AppUser user = users.save(new AppUser(email, passwordEncoder.encode("Password1!"), UserRole.PROVIDER));
        Provider provider = providers.save(new Provider(user, businessName, null, "wellness", "Roma", null));
        OfferedService service = offeredServices.save(new OfferedService(provider, "Servizio", null, 60, 5000));
        availabilities.save(new Availability(
                provider,
                service,
                (short) 1,
                LocalTime.of(9, 0),
                LocalTime.of(12, 0)
        ));
        return provider;
    }
}
