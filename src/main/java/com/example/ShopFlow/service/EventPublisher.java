package com.example.ShopFlow.service;
import com.example.ShopFlow.dto.DtoEstocBaixoEvent;
import com.example.ShopFlow.dto.DtoPedidoCriadoEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class EventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public EventPublisher(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void publicarPedidoCriado(DtoPedidoCriadoEvent event){
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend("pedido.criado", json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publicarEstoqueBaixo(DtoEstocBaixoEvent event){
        try {
            String json = objectMapper.writeValueAsString(event);
            redisTemplate.convertAndSend("estoque.baixo", json);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
