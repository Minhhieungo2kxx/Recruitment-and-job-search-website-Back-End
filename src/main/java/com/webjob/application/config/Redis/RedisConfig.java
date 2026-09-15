package com.webjob.application.config.Redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.webjob.application.dto.Request.Redis.PermissionSet;
import com.webjob.application.dto.record.JobRecommendationResponse;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;

import java.time.Duration;

@Configuration
@EnableCaching
public class RedisConfig {
    @Value("${spring.data.redis.standalone.host}")
    private String host;

    @Value("${spring.data.redis.standalone.port}")
    private int port;

    @Value("${spring.data.redis.password}")
    private String password;


    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config =
                new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(port);
        config.setPassword(password);
        return new LettuceConnectionFactory(config);
    }


    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory
            , ObjectMapper redisObjectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }


    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory
            , ObjectMapper redisObjectMapper) {

        RedisSerializationContext.SerializationPair<Object> pair =
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(redisObjectMapper));

        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(pair)
                .entryTtl(Duration.ofMinutes(8));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfig)
                .build();
    }

    //    dung cho phan quyen L1:Caffeine (local cache tren RAM)
    @Bean
    public Cache<String, PermissionSet> permissionLocalCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
    }

    //    dung cho phan quyen L2: Redis  (distributed cache) tren Server Redis,source cache chung
    @Bean
    public RedisTemplate<String, PermissionSet> permissionRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, PermissionSet> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        Jackson2JsonRedisSerializer<PermissionSet> valueSerializer =
                new Jackson2JsonRedisSerializer<>(PermissionSet.class);

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);

        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();

        return template;
    }

    //    dung cho JobRecommendation L1:Caffeine (local cache tren RAM)
    @Bean
    public Cache<String, JobRecommendationResponse> recommendationLocalCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(10))
                .build();
    }

    //    dung cho JobRecommendation L2: Redis(distributed cache) tren Server Redis,source cache chung
    @Bean
    public RedisTemplate<String, JobRecommendationResponse> recommendationRedisTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper) {

        RedisTemplate<String, JobRecommendationResponse> template =
                new RedisTemplate<>();

        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        Jackson2JsonRedisSerializer<JobRecommendationResponse> valueSerializer =
                new Jackson2JsonRedisSerializer<>(
                        redisObjectMapper,
                        JobRecommendationResponse.class
                );

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);

        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();

        return template;
    }

    // JobRecommendation version
    @Bean
    public RedisTemplate<String, Long> recommendationVersionRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        RedisTemplate<String, Long> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySerializer =
                new StringRedisSerializer();

        GenericToStringSerializer<Long> valueSerializer =
                new GenericToStringSerializer<>(Long.class);

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);

        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();

        return template;
    }


    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);

        return mapper;
    }

    @Bean
    public RedissonClient redissonClient() {

        Config config = new Config();

        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password);

        return Redisson.create(config);
    }


}

//Caffeine: Local Cache, nằm trên RAM của 1 máy chủ đơn lẻ, tốc độ siêu nhanh nhưng mất dữ liệu khi restart hoặc
//không chia sẻ được giữa các server.
//
//RedisTemplate (Redis): Distributed Cache (Cache phân tán), nằm ở một server/cluster Redis độc lập bên ngoài.
//Nhiều instance ứng dụng cùng đọc/ghi chung một kho dữ liệu Redis này, dữ liệu có thể được
//cấu hình lưu bền vững (persistent) và không bị mất khi ứng dụng khởi động lại.





