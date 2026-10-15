package com.example.ShopFlow.domain;


import com.example.ShopFlow.dto.DtoProduto;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "produtos")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150, nullable = false)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(nullable = false)
    private Integer estoque= 0;

    @Column(length = 80)
    private String categoria;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @PrePersist
    protected void onCreate(){
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate(){
        this.atualizadoEm = LocalDateTime.now();
    }

    public Produto() {}

    public  Produto(DtoProduto produto){
        this.nome = produto.nome();
        this.descricao = produto.descricao();
        this.preco = produto.preco();
        this.estoque = produto.estoque();
        this.categoria = produto.categoria();
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public void setEstoque(Integer estoque) {
        this.estoque = estoque;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public Integer getEstoque() {
        return estoque;
    }

    public String getCategoria() {
        return categoria;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }


    public Produto(Produto produtoExistente, Produto dadosNovos) {
        if (produtoExistente == null) {
            throw new IllegalArgumentException("Produto existente não pode ser nulo");
        }
        if (dadosNovos == null) {
            throw new IllegalArgumentException("Dados novos não podem ser nulos");
        }
        if (dadosNovos.getNome() == null || dadosNovos.getNome().isBlank()) {
            throw new IllegalArgumentException("Nome do produto não pode ser nulo ou vazio");
        }
        if (dadosNovos.getPreco() == null) {
            throw new IllegalArgumentException("Preço do produto não pode ser nulo");
        }
        if (dadosNovos.getEstoque() == null) {
            throw new IllegalArgumentException("Estoque do produto não pode ser nulo");
        }

        this.id = produtoExistente.getId();
        this.nome = dadosNovos.getNome();
        this.descricao = dadosNovos.getDescricao();
        this.preco = dadosNovos.getPreco();
        this.estoque = dadosNovos.getEstoque();
        this.categoria = dadosNovos.getCategoria();
        this.criadoEm = produtoExistente.getCriadoEm();
    }




}
