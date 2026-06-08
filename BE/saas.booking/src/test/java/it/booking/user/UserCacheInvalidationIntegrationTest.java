package it.booking.user;

import static it.booking.config.CacheNames.PROVIDER_DETAILS;
import static it.booking.config.CacheNames.PROVIDER_SEARCH;
import static it.booking.config.CacheNames.PROVIDER_SERVICES;
import static it.booking.config.CacheNames.SERVICE_DETAILS;
import static org.assertj.core.api.Assertions.assertThat;

import it.booking.provider.Provider;
import it.booking.provider.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = "spring.cache.type=simple")
class UserCacheInvalidationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private ProviderRepository providers;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cache(name).clear());
    }

    @Test
    void updatingCustomerDoesNotEvictCatalogCaches() {
        AppUser admin = saveUser("cache-admin@example.com", UserRole.ADMIN);
        AppUser customer = saveUser("cache-customer@example.com", UserRole.CUSTOMER);
        cache(PROVIDER_SEARCH).put("search", "cached");

        userService.update(
                customer.getId(),
                new UpdateUserRequest("cache-customer-updated@example.com", UserRole.CUSTOMER, true),
                admin.getId()
        );

        assertThat(cache(PROVIDER_SEARCH).get("search")).isNotNull();
    }

    @Test
    void disablingProviderEvictsItsPublicCatalogCaches() {
        AppUser admin = saveUser("cache-provider-admin@example.com", UserRole.ADMIN);
        AppUser providerUser = saveUser("cache-provider@example.com", UserRole.PROVIDER);
        Provider provider = providers.save(new Provider(
                providerUser,
                "Cache Studio",
                null,
                "wellness",
                "Roma",
                null
        ));
        cache(PROVIDER_SEARCH).put("search", "cached");
        cache(PROVIDER_DETAILS).put(provider.getId(), "provider");
        cache(PROVIDER_SERVICES).put(provider.getId(), "services");
        cache(SERVICE_DETAILS).put(new SimpleKey(provider.getId(), 11L), "service");

        userService.disable(providerUser.getId(), admin.getId());

        assertThat(cache(PROVIDER_SEARCH).get("search")).isNull();
        assertThat(cache(PROVIDER_DETAILS).get(provider.getId())).isNull();
        assertThat(cache(PROVIDER_SERVICES).get(provider.getId())).isNull();
        assertThat(cache(SERVICE_DETAILS).get(new SimpleKey(provider.getId(), 11L))).isNull();
    }

    private AppUser saveUser(String email, UserRole role) {
        return users.save(new AppUser(email, passwordEncoder.encode("Password1!"), role));
    }

    private Cache cache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            throw new IllegalStateException("Cache not configured: " + cacheName);
        }
        return cache;
    }
}
