package com.team14.amazon.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration for the Order Service.
 * Cache names and TTLs follow Section 4.4 + 8.1 of the M2 spec.
 */
@Configuration
@EnableCaching
public class RedisConfig {

    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        return mapper;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper()));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper()));
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(serializer))
                .disableCachingNullValues();

        // Per-cache TTLs (Section 8.1 + 4.4)
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();

        // M1 feature caches
        Duration f1Ttl  = Duration.ofMinutes(5);   // search results
        Duration f3Ttl  = Duration.ofMinutes(10);  // DTO
        Duration f5Ttl  = Duration.ofMinutes(5);   // JSONB query
        Duration f6Ttl  = Duration.ofMinutes(10);  // report
        Duration f8Ttl  = Duration.ofMinutes(15);  // relationship DTO
        Duration f9Ttl  = Duration.ofMinutes(10);  // combined
        Duration crudTtl = Duration.ofMinutes(15); // GET-by-ID

        cacheConfigs.put("order-service::S3-F1",  defaultConfig.entryTtl(f1Ttl));
        cacheConfigs.put("order-service::S3-F3",  defaultConfig.entryTtl(f3Ttl));
        cacheConfigs.put("order-service::S3-F5",  defaultConfig.entryTtl(f5Ttl));
        cacheConfigs.put("order-service::S3-F6",  defaultConfig.entryTtl(f6Ttl));
        cacheConfigs.put("order-service::S3-F9",  defaultConfig.entryTtl(f9Ttl));

        // M2 feature caches
        cacheConfigs.put("order-service::S3-F10", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("order-service::S3-F12", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // CRUD GET-by-ID
        cacheConfigs.put("order-service::order",      defaultConfig.entryTtl(crudTtl));
        cacheConfigs.put("order-service::order-item", defaultConfig.entryTtl(crudTtl));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
