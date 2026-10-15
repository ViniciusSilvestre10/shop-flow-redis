package com.example.ShopFlow.service;

import com.example.ShopFlow.dto.DtoItemCarrinhoResponse;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;



@Service
public class CarrinhoService {

    private final HashOperations<String, String, String> hashOps;
    private final StringRedisTemplate redisTemplate;
    private static final Duration TTL_CARRINHO = Duration.ofMinutes(30);

    public CarrinhoService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.hashOps = redisTemplate.opsForHash();
    }

    private String chave(Long userId) {
        return "carrinho:" + userId;
    }

    public void adicionarItem(Long userId, Long produtoId, int quantidade) {
        String chave = chave(userId);
        hashOps.put(chave, produtoId.toString(), String.valueOf(quantidade));
        redisTemplate.expire(chave, TTL_CARRINHO);
    }

    public List<DtoItemCarrinhoResponse> buscarCarrinho(Long userId) {
        Map<String, String> dadoCarrinho = hashOps.entries(chave(userId));

        return dadoCarrinho.entrySet().stream().map(e-> new DtoItemCarrinhoResponse(
                Long.parseLong(e.getKey()), Integer.parseInt(e.getValue()))).toList();
    }

    public void removerItem(Long userId, Long produtoId) {
        hashOps.delete(chave(userId), produtoId.toString());
    }
}