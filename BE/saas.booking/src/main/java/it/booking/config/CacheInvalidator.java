package it.booking.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class CacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidator.class);

    private final CacheManager cacheManager;

    public CacheInvalidator(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void providerSearchChanged() {
        afterCommit(() -> clear(CacheNames.PROVIDER_SEARCH));
    }

    public void providerChanged(Long providerId) {
        afterCommit(() -> {
            clear(CacheNames.PROVIDER_SEARCH);
            evict(CacheNames.PROVIDER_DETAILS, providerId);
        });
    }

    public void providerVisibilityChanged(Long providerId) {
        afterCommit(() -> {
            clear(CacheNames.PROVIDER_SEARCH);
            evict(CacheNames.PROVIDER_DETAILS, providerId);
            evict(CacheNames.PROVIDER_SERVICES, providerId);
            clear(CacheNames.SERVICE_DETAILS);
        });
    }

    public void serviceChanged(Long providerId, Long serviceId) {
        afterCommit(() -> {
            clear(CacheNames.PROVIDER_SEARCH);
            evict(CacheNames.PROVIDER_SERVICES, providerId);
            evict(CacheNames.SERVICE_DETAILS, new SimpleKey(providerId, serviceId));
        });
    }

    private void afterCommit(Runnable invalidation) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidation.run();
                }
            });
            return;
        }
        invalidation.run();
    }

    private void evict(String cacheName, Object key) {
        Cache cache = cache(cacheName);
        if (cache == null) {
            return;
        }
        try {
            cache.evict(key);
        } catch (RuntimeException exception) {
            log.warn("Cache eviction failed; domain write remains authoritative: cache={}, key={}",
                    cacheName, key, exception);
        }
    }

    private void clear(String cacheName) {
        Cache cache = cache(cacheName);
        if (cache == null) {
            return;
        }
        try {
            cache.clear();
        } catch (RuntimeException exception) {
            log.warn("Cache clear failed; domain write remains authoritative: cache={}",
                    cacheName, exception);
        }
    }

    private Cache cache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.error("Cache is not configured; skipping invalidation: cache={}", cacheName);
        }
        return cache;
    }
}
