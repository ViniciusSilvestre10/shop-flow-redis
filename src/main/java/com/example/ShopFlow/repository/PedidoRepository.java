package com.example.ShopFlow.repository;

import com.example.ShopFlow.domain.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
