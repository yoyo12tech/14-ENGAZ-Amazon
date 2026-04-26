package com.team14.amazon.shipping.config;

import java.time.Duration;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {

    @Bean
    @SuppressWarnings("removal")
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));

        return RedisCacheManager.builder(cf)
                .cacheDefaults(defaults)
                .withCacheConfiguration("shipping-service::shipment", defaults.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration("shipping-service::S4-F1", defaults.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("shipping-service::S4-F3", defaults.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("shipping-service::S4-F6", defaults.entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("shipping-service::S4-F8", defaults.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration("shipping-service::S4-F9", defaults.entryTtl(Duration.ofMinutes(10)))
                .build();
    }

    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
}
