package it.booking.config;

import static org.assertj.core.api.Assertions.assertThat;

import it.booking.catalog.CatalogProviderResponse;
import it.booking.catalog.CatalogServiceResponse;
import it.booking.common.PageResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

class RedisConfigTest {

    private final GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();

    @Test
    void serializesAndDeserializesProviderSearchResponse() {
        PageResponse<CatalogProviderResponse> response = new PageResponse<>(
                List.of(new CatalogProviderResponse(
                        42L,
                        "Studio Test",
                        "Descrizione",
                        "wellness",
                        "Roma",
                        "Via Roma 1"
                )),
                0,
                20,
                1,
                1,
                true,
                true
        );

        Object restored = serializer.deserialize(serializer.serialize(response));

        assertThat(restored).isEqualTo(response);
    }

    @Test
    void serializesAndDeserializesProviderServices() {
        List<CatalogServiceResponse> services = new ArrayList<>(List.of(new CatalogServiceResponse(
                11L,
                42L,
                "Massaggio",
                "Sessione",
                60,
                7000
        )));

        Object restored = serializer.deserialize(serializer.serialize(services));

        assertThat(restored).isEqualTo(services);
    }

    @Test
    void configuresSpecificTtlForEachCache() {
        CacheTtlProperties ttl = new CacheTtlProperties();
        RedisConfig config = new RedisConfig(ttl);
        Map<String, RedisCacheConfiguration> configurations =
                config.cacheConfigurations(config.cacheConfiguration());

        assertThat(configurations.get(CacheNames.PROVIDER_SEARCH).getTtl()).isEqualTo(Duration.ofMinutes(5));
        assertThat(configurations.get(CacheNames.PROVIDER_DETAILS).getTtl()).isEqualTo(Duration.ofMinutes(15));
        assertThat(configurations.get(CacheNames.PROVIDER_SERVICES).getTtl()).isEqualTo(Duration.ofMinutes(10));
        assertThat(configurations.get(CacheNames.SERVICE_DETAILS).getTtl()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void configuresFailOpenCacheErrorHandling() {
        RedisConfig config = new RedisConfig(new CacheTtlProperties());

        CacheErrorHandler errorHandler = config.errorHandler();

        assertThat(errorHandler).isInstanceOf(FailOpenCacheErrorHandler.class);
    }
}
