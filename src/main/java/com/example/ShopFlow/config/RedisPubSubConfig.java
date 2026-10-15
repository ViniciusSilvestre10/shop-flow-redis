package com.example.ShopFlow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Component;

@Component
public class RedisPubSubConfig {

    @Bean
    public MessageListenerAdapter listenerAdapterPedido(NotificationWorker worker) {
        return new MessageListenerAdapter(worker, "handlePedidoCriado");
    }

    @Bean
    public MessageListenerAdapter listenerAdapterEstoque(NotificationWorker worker) {
        return new MessageListenerAdapter(worker, "handleEstoqueBaixo");
    }

    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapterPedido,
            MessageListenerAdapter listenerAdapterEstoque){

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(listenerAdapterPedido, new ChannelTopic("pedido.criado"));
        container.addMessageListener(listenerAdapterEstoque, new ChannelTopic("estoque.baixo"));

        return container;
    }
}
