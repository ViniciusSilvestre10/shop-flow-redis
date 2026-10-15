package com.example.ShopFlow.controller;

import com.example.ShopFlow.domain.Produto;
import com.example.ShopFlow.domain.Usuario;
import com.example.ShopFlow.repository.ProdutoRepository;
import com.example.ShopFlow.repository.UsuarioRepository;
import com.example.ShopFlow.service.CheckoutService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CheckoutControllerTest {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void deveGarantirQueApenasUmPedidoSejaConfirmadoSobConcorrencia() throws InterruptedException {
        // Produto de teste com apenas 1 item em estoque

        Usuario u1 = new Usuario();
        u1.setNome("Teste 1");
        u1.setEmail("teste1-" + UUID.randomUUID() + "@teste.com");
        u1.setSenhaHash("hash");
        u1 = usuarioRepository.save(u1);

        Usuario u2 = new Usuario();
        u2.setNome("Teste 2");
        u2.setEmail("teste2-" + UUID.randomUUID() + "@teste.com");
        u2.setSenhaHash("hash");
        u2 = usuarioRepository.save(u2);


        Produto produto = new Produto();
        produto.setNome("Item Limitado");
        produto.setPreco(new BigDecimal("150.00"));
        produto.setEstoque(1);
        produto = produtoRepository.save(produto);

        Long produtoId = produto.getId();
        int threadsSimultaneas = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadsSimultaneas);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadsSimultaneas);

        AtomicInteger sucessos = new AtomicInteger(0);
        AtomicInteger falhasConcorrencia = new AtomicInteger(0);

        for (int i = 0; i < threadsSimultaneas; i++) {
            final long userId = i + 1;
            executorService.submit(() -> {
                try {
                    startLatch.await(); // Aguarda a liberação conjunta
                    checkoutService.realizarCheckout(userId, produtoId, 1);
                    sucessos.incrementAndGet();
                } catch (Exception e) {
                    falhasConcorrencia.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Dispara as duas threads exatamente ao mesmo tempo
        doneLatch.await(); // Aguarda a execução de ambas

        // Confirmações: exatamente 1 compra aprovada e 1 travada pelo lock
        assertEquals(1, sucessos.get(), "Apenas uma compra deveria ser processada.");
        assertEquals(1, falhasConcorrencia.get(), "Uma das requisições deveria falhar no lock.");

        // O estoque deve terminar zerado, sem valores negativos
        Produto produtoFinal = produtoRepository.findById(produtoId).orElseThrow();
        assertEquals(0, produtoFinal.getEstoque(), "O estoque final deve ser zero.");
    }



}