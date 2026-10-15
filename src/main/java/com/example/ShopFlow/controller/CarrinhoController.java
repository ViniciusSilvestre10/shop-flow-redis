package com.example.ShopFlow.controller;


import com.example.ShopFlow.domain.Produto;
import com.example.ShopFlow.dto.DtoItemCarrinho;
import com.example.ShopFlow.dto.DtoItemCarrinhoResponse;
import com.example.ShopFlow.service.CarrinhoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/carrinho")
public class CarrinhoController {

    private final CarrinhoService service;

    public CarrinhoController(CarrinhoService service) {
        this.service = service;
    }

    @GetMapping("/{userId}")
    public List<DtoItemCarrinhoResponse> buscarItemCarrinho(@PathVariable Long userId) {
         return service.buscarCarrinho(userId);
    }

    @PostMapping("/{userId}/itens")
    public ResponseEntity<Void> adicionarItem(@PathVariable Long userId,
                                              @Valid @RequestBody DtoItemCarrinho request) {

        service.adicionarItem(userId, request.produtoId(), request.quantidade());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{userId}/itens/{produtoId}")
    public ResponseEntity<Void> deletar(@PathVariable Long userId, @PathVariable Long produtoId) {
        service.removerItem(userId, produtoId);
        return ResponseEntity.noContent().build();
    }


}
