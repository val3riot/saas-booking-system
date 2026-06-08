package it.booking.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCache;

class FailOpenCacheErrorHandlerTest {

    private final FailOpenCacheErrorHandler errorHandler = new FailOpenCacheErrorHandler();
    private final Cache cache = new ConcurrentMapCache("test");
    private final RuntimeException failure = new RuntimeException("Redis unavailable");

    @Test
    void ignoresAllCacheOperationFailures() {
        assertThatCode(() -> errorHandler.handleCacheGetError(failure, cache, "key"))
                .doesNotThrowAnyException();
        assertThatCode(() -> errorHandler.handleCachePutError(failure, cache, "key", "value"))
                .doesNotThrowAnyException();
        assertThatCode(() -> errorHandler.handleCacheEvictError(failure, cache, "key"))
                .doesNotThrowAnyException();
        assertThatCode(() -> errorHandler.handleCacheClearError(failure, cache))
                .doesNotThrowAnyException();
    }
}
