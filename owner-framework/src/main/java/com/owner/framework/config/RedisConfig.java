package com.owner.framework.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.time.Duration;
import java.util.Arrays;

/**
 * Redis 配置：Spring Cache 使用 REDIS 实现（{@code spring.cache.type=redis}）。
 * <p>序列化采用 Spring Data Redis 4 的 Jackson 3 实现
 * （{@link GenericJacksonJsonRedisSerializer}），与 Spring Boot 4 默认 JSON 栈保持一致。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableCaching
public class RedisConfig {

    private static final Duration DEFAULT_TTL = Duration.ofHours(2);

    private static final String CACHE_KEY_PREFIX = "owner:cache:";

    /**
     * 供业务使用的通用 RedisTemplate（Key 为 String，Value 为 JSON）。
     */
    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        RedisSerializer<Object> valueSerializer = jsonSerializer();

        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 缓存管理器：REDIS + JSON 序列化 + 统一 TTL 与 Key 前缀。
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .computePrefixWith(cacheName -> CACHE_KEY_PREFIX + cacheName + ":")
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(jsonSerializer()))
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }

    /**
     * 自定义缓存 Key 生成器：类名.方法名(参数...)，避免不同参数产生相同 Key。
     */
    @Bean("cacheKeyGenerator")
    public KeyGenerator cacheKeyGenerator() {
        return (target, method, params) -> method.getDeclaringClass().getSimpleName()
                + "." + method.getName() + "(" + Arrays.deepToString(params) + ")";
    }

    /**
     * Jackson 3 版通用序列化器：写入类型信息，保证反序列化能还原原始类型
     * （否则缓存值会退化成 LinkedHashMap 导致类型转换异常）。
     */
    private RedisSerializer<Object> jsonSerializer() {
        return GenericJacksonJsonRedisSerializer.builder()
                .enableDefaultTyping(BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build())
                .build();
    }
}
