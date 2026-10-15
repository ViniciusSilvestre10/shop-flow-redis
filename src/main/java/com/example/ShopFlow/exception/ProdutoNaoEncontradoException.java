package com.example.ShopFlow.exception;

public class ProdutoNaoEncontradoException extends RuntimeException{

    public ProdutoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    public ProdutoNaoEncontradoException(Long id) {
        super("Produto não encontrado com o id: " + id);
    }

}
