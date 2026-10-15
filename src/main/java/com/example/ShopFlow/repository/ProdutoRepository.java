package com.example.ShopFlow.repository;

import com.example.ShopFlow.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
}
