package com.example.ShopFlow.dto;

import java.math.BigDecimal;

public record DtoPedidoCriadoEvent(Long pedidoId, Long usuarioId, BigDecimal total) {
}
