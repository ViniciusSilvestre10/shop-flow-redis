package com.example.ShopFlow.dto;

import jakarta.persistence.Column;

import java.math.BigDecimal;

public record DtoProduto(String nome, String descricao, BigDecimal preco, Integer estoque, String categoria) {
}
