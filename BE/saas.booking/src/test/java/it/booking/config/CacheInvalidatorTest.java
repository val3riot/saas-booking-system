package it.booking.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class CacheInvalidatorTest {

    private final ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(
            CacheNames.PROVIDER_DETAILS,
            CacheNames.PROVIDER_SEARCH,
            CacheNames.PROVIDER_SERVICES,
            CacheNames.SERVICE_DETAILS
    );
    private final CacheInvalidator cacheInvalidator = new CacheInvalidator(cacheManager);

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void serviceChangedEvictsOnlyRelatedServiceEntriesAndProviderSearch() {
        cache(CacheNames.PROVIDER_SEARCH).put("search", "cached");
        cache(CacheNames.PROVIDER_SERVICES).put(7L, "services");
        cache(CacheNames.PROVIDER_SERVICES).put(8L, "other services");
        cache(CacheNames.SERVICE_DETAILS).put(new SimpleKey(7L, 11L), "service");
        cache(CacheNames.SERVICE_DETAILS).put(new SimpleKey(8L, 12L), "other service");

        cacheInvalidator.serviceChanged(7L, 11L);

        assertThat(cache(CacheNames.PROVIDER_SEARCH).get("search")).isNull();
        assertThat(cache(CacheNames.PROVIDER_SERVICES).get(7L)).isNull();
        assertThat(cache(CacheNames.PROVIDER_SERVICES).get(8L)).isNotNull();
        assertThat(cache(CacheNames.SERVICE_DETAILS).get(new SimpleKey(7L, 11L))).isNull();
        assertThat(cache(CacheNames.SERVICE_DETAILS).get(new SimpleKey(8L, 12L))).isNotNull();
    }

    @Test
    void providerVisibilityChangedEvictsAllPublicDataForProvider() {
        cache(CacheNames.PROVIDER_DETAILS).put(7L, "provider");
        cache(CacheNames.PROVIDER_SERVICES).put(7L, "services");
        cache(CacheNames.SERVICE_DETAILS).put(new SimpleKey(7L, 11L), "service");

        cacheInvalidator.providerVisibilityChanged(7L);

        assertThat(cache(CacheNames.PROVIDER_DETAILS).get(7L)).isNull();
        assertThat(cache(CacheNames.PROVIDER_SERVICES).get(7L)).isNull();
        assertThat(cache(CacheNames.SERVICE_DETAILS).get(new SimpleKey(7L, 11L))).isNull();
    }

    @Test
    void cacheFailuresDoNotBlockDomainInvalidationFlow() {
        SimpleCacheManager failingCacheManager = new SimpleCacheManager();
        failingCacheManager.setCaches(List.of(
                new FailingCache(CacheNames.PROVIDER_SEARCH),
                new FailingCache(CacheNames.PROVIDER_DETAILS),
                new FailingCache(CacheNames.PROVIDER_SERVICES),
                new FailingCache(CacheNames.SERVICE_DETAILS)
        ));
        failingCacheManager.initializeCaches();
        CacheInvalidator failingInvalidator = new CacheInvalidator(failingCacheManager);

        assertThatCode(() -> failingInvalidator.providerVisibilityChanged(7L))
                .doesNotThrowAnyException();
        assertThatCode(() -> failingInvalidator.serviceChanged(7L, 11L))
                .doesNotThrowAnyException();
    }

    @Test
    void invalidationRunsOnlyAfterTransactionCommit() {
        cache(CacheNames.PROVIDER_SEARCH).put("search", "cached");
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();

        cacheInvalidator.providerSearchChanged();

        assertThat(cache(CacheNames.PROVIDER_SEARCH).get("search")).isNotNull();
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertThat(cache(CacheNames.PROVIDER_SEARCH).get("search")).isNull();
    }

    private Cache cache(String cacheName) {
        return cacheManager.getCache(cacheName);
    }

    private static final class FailingCache extends ConcurrentMapCache {

        private FailingCache(String name) {
            super(name);
        }

        @Override
        public void evict(Object key) {
            throw new IllegalStateException("Redis unavailable");
        }

        @Override
        public void clear() {
            throw new IllegalStateException("Redis unavailable");
        }
    }
}
