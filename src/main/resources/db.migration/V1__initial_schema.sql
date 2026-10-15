CREATE TABLE produtos (
                          id BIGSERIAL PRIMARY KEY,
                          nome VARCHAR(150) NOT NULL,
                          descricao TEXT,
                          preco NUMERIC(10,2) NOT NULL,
                          estoque INTEGER NOT NULL DEFAULT 0,
                          categoria VARCHAR(80),
                          criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
                          atualizado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE usuarios (
                          id BIGSERIAL PRIMARY KEY,
                          nome VARCHAR(120) NOT NULL,
                          email VARCHAR(150) UNIQUE NOT NULL,
                          senha_hash VARCHAR(255) NOT NULL,
                          criado_em TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE pedidos (
                         id BIGSERIAL PRIMARY KEY,
                         usuario_id BIGINT NOT NULL,
                         status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
                         total NUMERIC(10,2) NOT NULL,
                         criado_em TIMESTAMP NOT NULL DEFAULT NOW(),
                         CONSTRAINT fk_pedidos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

CREATE TABLE itens_pedido (
                              id BIGSERIAL PRIMARY KEY,
                              pedido_id BIGINT NOT NULL,
                              produto_id BIGINT NOT NULL,
                              quantidade INTEGER NOT NULL,
                              preco_unitario NUMERIC(10,2) NOT NULL,
                              CONSTRAINT fk_itens_pedido_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
                              CONSTRAINT fk_itens_pedido_produto FOREIGN KEY (produto_id) REFERENCES produtos(id)
);

CREATE TABLE cupons (
                        id BIGSERIAL PRIMARY KEY,
                        codigo VARCHAR(30) UNIQUE NOT NULL,
                        percentual_desconto NUMERIC(5,2) NOT NULL,
                        valido_ate TIMESTAMP NOT NULL,
                        ativo BOOLEAN NOT NULL DEFAULT TRUE
);


CREATE INDEX idx_pedidos_usuario ON pedidos(usuario_id);
CREATE INDEX idx_itens_pedido_pedido ON itens_pedido(pedido_id);
CREATE INDEX idx_itens_pedido_produto ON itens_pedido(produto_id);