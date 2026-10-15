package com.example.ShopFlow.service;

import com.example.ShopFlow.domain.Pedido;
import com.example.ShopFlow.domain.Produto;
import com.example.ShopFlow.domain.StatusPedido;
import com.example.ShopFlow.domain.Usuario;
import com.example.ShopFlow.dto.DtoEstocBaixoEvent;
import com.example.ShopFlow.dto.DtoPedidoCriadoEvent;
import com.example.ShopFlow.exception.LockAcquisitionException;
import com.example.ShopFlow.exception.OutOfStockException;
import com.example.ShopFlow.repository.PedidoRepository;
import com.example.ShopFlow.repository.ProdutoRepository;
import com.example.ShopFlow.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class CheckoutService {

    private final DistributedLockService lockService;
    private final ProdutoRepository produtoRepository;
    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CheckoutService(DistributedLockService lockService,
                           ProdutoRepository produtoRepository,
                           PedidoRepository pedidoRepository,
                           UsuarioRepository usuarioRepository,
                           StringRedisTemplate redisTemplate) {
        this.lockService = lockService;
        this.produtoRepository = produtoRepository;
        this.pedidoRepository = pedidoRepository;
        this.usuarioRepository = usuarioRepository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public Pedido realizarCheckout(Long userId, Long produtoId, int quantidade) {
        String lockKey = "lock:produto:" + produtoId;
        String token = UUID.randomUUID().toString();
        long lockTimeoutSeconds = 5;

        boolean lockAdquirido = lockService.acquireLock(lockKey, token, lockTimeoutSeconds);

        if (!lockAdquirido) {
            throw new LockAcquisitionException("Não foi possível processar a compra no momento devido à alta concorrência. Tente novamente.");
        }

        try {
            Usuario usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

            Produto produto = produtoRepository.findById(produtoId)
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado."));

            if (produto.getEstoque() < quantidade) {
                throw new OutOfStockException("Estoque insuficiente para o produto: " + produto.getNome());
            }

            produto.setEstoque(produto.getEstoque() - quantidade);
            produtoRepository.save(produto);

            Pedido pedido = new Pedido();
            pedido.setUsuario(usuario);
            pedido.setTotal(produto.getPreco().multiply(BigDecimal.valueOf(quantidade)));
            pedido.setStatus(StatusPedido.CONFIRMADO);
            Pedido pedidoSalvo = pedidoRepository.save(pedido);
            registrarVendaNoRanking(produtoId, quantidade);

            try {
                DtoPedidoCriadoEvent eventoPedido = new DtoPedidoCriadoEvent(pedidoSalvo.getId(), userId, pedidoSalvo.getTotal());
                String jsonPedido = objectMapper.writeValueAsString(eventoPedido);
                redisTemplate.convertAndSend("pedido.criado", jsonPedido);

                if (produto.getEstoque() <= 2) {
                    DtoEstocBaixoEvent eventoEstoque = new DtoEstocBaixoEvent(produto.getId(), produto.getNome(), produto.getEstoque());
                    String jsonEstoque = objectMapper.writeValueAsString(eventoEstoque);
                    redisTemplate.convertAndSend("estoque.baixo", jsonEstoque);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return pedidoSalvo;

        } finally {
            lockService.releaseLock(lockKey, token);
        }
    }


    public void registrarVendaNoRanking(Long produtoId, int quantidade){
        String chaveRanking = "ranking:produtos:vendas";

        redisTemplate.opsForZSet().incrementScore(chaveRanking, produtoId.toString(), quantidade);
    }
}