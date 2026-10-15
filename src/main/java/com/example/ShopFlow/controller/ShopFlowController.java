package com.example.ShopFlow.controller;

import com.example.ShopFlow.domain.Produto;
import com.example.ShopFlow.dto.DtoProduto;
import com.example.ShopFlow.service.ShopFlowService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/produtos")
@Validated
public class ShopFlowController {

    private ShopFlowService service;

    public ShopFlowController(ShopFlowService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Produto> buscarProduto (@PathVariable Long id) {
        Produto produto = service.buscarProduto(id);
        return ResponseEntity.ok(produto);
    }

    @PostMapping
    public ResponseEntity<Produto> criar(@RequestBody DtoProduto dto) {
        Produto produto = service.criarProduto(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(produto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Produto> atualizar(@PathVariable Long id, @RequestBody Produto produto) {
        Produto produtoAtualizado = service.atualizar(id, produto);
        return ResponseEntity.ok(produtoAtualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessao-teste")
    public ResponseEntity<String> testarSessao(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        session.setAttribute("teste", "valor-" + System.currentTimeMillis());
        return ResponseEntity.ok("Session ID: " + session.getId());
    }


    @GetMapping("/mais-vendidos")
    public ResponseEntity<Set<String>> buscarProdutoMaisVendidos(@RequestParam(defaultValue = "10") @Min(1) @Max(50) int limite){
        Set<String> maisVendidos = service.obterMaisVendidos(limite);
        return ResponseEntity.ok(maisVendidos);
    }




}
