package it.booking.config;

import java.util.Map;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@EnableConfigurationProperties(CacheTtlProperties.class)
public class RedisConfig implements CachingConfigurer {

    private final CacheTtlProperties ttl;

    public RedisConfig(CacheTtlProperties ttl) {
        this.ttl = ttl;
    }

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer cacheManagerCustomizer(RedisCacheConfiguration defaultConfiguration) {
        return builder -> builder
                .disableCreateOnMissingCache()
                .withInitialCacheConfigurations(cacheConfigurations(defaultConfiguration));
    }

    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new FailOpenCacheErrorHandler();
    }

    Map<String, RedisCacheConfiguration> cacheConfigurations(RedisCacheConfiguration defaultConfiguration) {
        return Map.of(
                CacheNames.PROVIDER_SEARCH, defaultConfiguration.entryTtl(ttl.getProviderSearch()),
                CacheNames.PROVIDER_DETAILS, defaultConfiguration.entryTtl(ttl.getProviderDetails()),
                CacheNames.PROVIDER_SERVICES, defaultConfiguration.entryTtl(ttl.getProviderServices()),
                CacheNames.SERVICE_DETAILS, defaultConfiguration.entryTtl(ttl.getServiceDetails())
        );
    }
}
