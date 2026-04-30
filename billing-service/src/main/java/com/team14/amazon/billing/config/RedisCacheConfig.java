package com.team14.amazon.billing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.team14.amazon.billing.cache.BillingCacheNames;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class RedisCacheConfig {

    @Bean
    @SuppressWarnings("removal")
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        RedisSerializationContext.SerializationPair<Object> serializer =
                RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper)
                );

        RedisCacheConfiguration defaultConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(serializer)
                .disableCachingNullValues();

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withCacheConfiguration(BillingCacheNames.S5_F1, defaultConfiguration.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration(BillingCacheNames.S5_F3, defaultConfiguration.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(BillingCacheNames.S5_F6, defaultConfiguration.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(BillingCacheNames.S5_F8, defaultConfiguration.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration(BillingCacheNames.S5_F9, defaultConfiguration.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration(BillingCacheNames.TRANSACTION, defaultConfiguration.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration(BillingCacheNames.VOUCHER, defaultConfiguration.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration(BillingCacheNames.TRANSACTION_VOUCHER,
                        defaultConfiguration.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration(BillingCacheNames.TRANSACTION_LIFECYCLE,
                        defaultConfiguration.entryTtl(Duration.ofMinutes(10)))
                .build();
    }
}

