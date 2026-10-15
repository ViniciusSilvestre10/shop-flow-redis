package com.example.ShopFlow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DtoItemCarrinho(
        @NotNull long produtoId,
        @Positive int quantidade
) {
}
