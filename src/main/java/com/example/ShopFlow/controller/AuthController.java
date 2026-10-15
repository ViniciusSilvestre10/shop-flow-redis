package com.example.ShopFlow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record LoginRequest(String email, String senha) {}

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody(required = false) LoginRequest request) {
        return ResponseEntity.ok(Map.of(
                "status", "sucesso",
                "mensagem", "Login efetuado com sucesso (simulação para validação)."
        ));
    }
}