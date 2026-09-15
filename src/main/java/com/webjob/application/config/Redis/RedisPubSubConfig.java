package com.webjob.application.config.Redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webjob.application.pubsub.subscriber.*;
import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {



    private MessageListenerAdapter createAdapter(Object subscriber, ObjectMapper redisObjectMapper) {

        MessageListenerAdapter adapter =
                new MessageListenerAdapter(subscriber, "receiveMessage");

        adapter.setSerializer(
                new GenericJackson2JsonRedisSerializer(redisObjectMapper)
        );

        return adapter;
    }

    @Bean
    public MessageListenerAdapter chatPublicAdapter(ChatPublicSubscriber subscriber,ObjectMapper redisObjectMapper) {
        return createAdapter(subscriber,redisObjectMapper);
    }


    @Bean
    public MessageListenerAdapter chatPrivateAdapter(ChatPrivateSubscriber subscriber,ObjectMapper redisObjectMapper) {
        return createAdapter(subscriber,redisObjectMapper);
    }


    @Bean
    public MessageListenerAdapter presenceAdapter(PresenceSubscriber subscriber,ObjectMapper redisObjectMapper) {
        return createAdapter(subscriber,redisObjectMapper);
    }


    @Bean
    public MessageListenerAdapter notificationAdapter(NotificationSubscriber subscriber,ObjectMapper redisObjectMapper) {
        return createAdapter(subscriber,redisObjectMapper);
    }

    @Bean
    public MessageListenerAdapter recommendationCacheAdapter(RecommendationCacheSubscriber subscriber,ObjectMapper redisObjectMapper
    ) {
        return createAdapter(subscriber, redisObjectMapper);
    }
    @Bean
    public MessageListenerAdapter permissionCacheAdapter(
            PermissionCacheSubscriber subscriber,
            ObjectMapper redisObjectMapper) {

        return createAdapter(subscriber, redisObjectMapper);
    }


    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter chatPublicAdapter,
            MessageListenerAdapter chatPrivateAdapter,
            MessageListenerAdapter presenceAdapter,
            MessageListenerAdapter notificationAdapter,
            MessageListenerAdapter recommendationCacheAdapter,
            MessageListenerAdapter permissionCacheAdapter) {

        RedisMessageListenerContainer container =
                new RedisMessageListenerContainer();

        container.setConnectionFactory(connectionFactory);

        container.addMessageListener(
                chatPublicAdapter,
                new ChannelTopic("chat.public")
        );

        container.addMessageListener(
                chatPrivateAdapter,
                new ChannelTopic("chat.private")
        );

        container.addMessageListener(
                presenceAdapter,
                new ChannelTopic("presence")
        );

        container.addMessageListener(
                notificationAdapter,
                new ChannelTopic("notification")
        );

        container.addMessageListener(
                recommendationCacheAdapter,
                new ChannelTopic(
                        "recommendation.invalidate"
                )
        );
        container.addMessageListener(
                permissionCacheAdapter,
                new ChannelTopic("permission.invalidate")
        );


        return container;
    }

}
