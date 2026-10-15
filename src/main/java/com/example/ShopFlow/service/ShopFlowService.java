package com.example.ShopFlow.service;

import com.example.ShopFlow.domain.Produto;
import com.example.ShopFlow.dto.DtoProduto;
import com.example.ShopFlow.exception.ProdutoNaoEncontradoException;
import com.example.ShopFlow.repository.ProdutoRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ShopFlowService {

    private ProdutoRepository produtoRepository;
    private final StringRedisTemplate redisTemplate;

    public ShopFlowService(StringRedisTemplate redisTemplate, ProdutoRepository produtoRepository) {
        this.redisTemplate = redisTemplate;
        this.produtoRepository = produtoRepository;
    }

    @Cacheable(value = "produtos", key = "#id")
    public Produto buscarProduto(Long id) {
        Produto produtoEncontrado = produtoRepository.findById(id).orElseThrow(() -> new ProdutoNaoEncontradoException(id));
        return  produtoEncontrado;
    }

    @CacheEvict(value = "produtos", key = "#id", beforeInvocation = true)
    public void deletar(Long id) {
        produtoRepository.deleteById(id);
    }

    @CacheEvict(value = "produtos", key = "#id", beforeInvocation = true)
    public Produto atualizar(Long id, Produto produto) {
        Produto produtoEncontrado = produtoRepository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));

        Produto produtoAtualizado = new Produto(produtoEncontrado, produto);

        return produtoRepository.save(produtoAtualizado);
    }

    @CachePut(cacheNames = "produtos", key = "#result.id")
    public Produto criarProduto(DtoProduto dto) {
        Produto produtoNovo = new Produto(dto);
        return produtoRepository.save(produtoNovo);
    }

    public Set<String> obterMaisVendidos(int limite){
        String chaveRanking = "ranking:produtos:vendas";
        long fim = limite > 0 ? limite - 1 : 0;
        return redisTemplate.opsForZSet().reverseRange(chaveRanking, 0, fim);
    }

}
