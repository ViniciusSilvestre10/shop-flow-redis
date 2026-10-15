package com.example.ShopFlow.controller;

import com.example.ShopFlow.domain.Pedido;
import com.example.ShopFlow.dto.CheckoutDto;
import com.example.ShopFlow.exception.LockAcquisitionException;
import com.example.ShopFlow.exception.OutOfStockException;
import com.example.ShopFlow.service.CheckoutService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<?> checkout(@PathVariable Long userId, @RequestBody CheckoutDto request){
        try {
            Pedido pedido = checkoutService.realizarCheckout(userId, request.produtoId(), request.quantidade());
            return ResponseEntity.status(HttpStatus.CREATED).body(pedido);
        }catch (LockAcquisitionException e){
            return  ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Concorrência Detectada", "message", e.getMessage()));
        }catch (OutOfStockException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Estoque Esgotado", "message", e.getMessage()));
        }
    }
}
