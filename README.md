# Shop Flow — Plataforma de E-commerce com Redis e Spring Boot

Projeto de estudo aplicado, construído para consolidar o uso de Redis em cenários reais do mercado de trabalho: cache, sessões, rate limiting, locks distribuídos, filas de eventos e rankings em tempo real.

---

## Índice

1. [Visão Geral](#visão-geral)
2. [Problema Real que o Projeto Resolve](#problema-real-que-o-projeto-resolve)
3. [Por que Redis para cada problema](#por-que-redis-para-cada-problema-teoria)
4. [Modelagem do Banco de Dados (PostgreSQL)](#modelagem-do-banco-de-dados-postgresql)
5. [O que fica só no Redis](#o-que-fica-só-no-redis)
6. [Arquitetura](#arquitetura)
7. [Stack Técnica](#stack-técnica)
8. [Principais Endpoints](#principais-endpoints)
9. [Roteiro de Implementação — Passo a Passo](#roteiro-de-implementação--passo-a-passo)
10. [Roteiro de Testes por Funcionalidade](#roteiro-de-testes-por-funcionalidade)
11. [Como Executar](#como-executar)
12. [Próximos Passos](#próximos-passos)

---

## Visão Geral

Shop Flow simula o backend de um e-commerce de médio porte, focado em um problema muito comum no dia a dia de desenvolvedores backend: garantir performance e consistência em um sistema com alta concorrência de leitura e escrita, sem sobrecarregar o banco relacional principal.

Em vez de tratar o Redis como "só um cache", o projeto usa cada uma de suas estruturas de dados para resolver um problema de negócio específico — a forma como ele é realmente utilizado em produção.

---

## Problema Real que o Projeto Resolve

Em um e-commerce, três dores são recorrentes:

1. **Catálogo consultado com muito mais frequência do que é atualizado** — cada requisição indo ao banco relacional gera latência e custo desnecessário.
2. **Picos de tráfego em promoções** — múltiplos usuários tentam comprar o último item em estoque ao mesmo tempo, gerando race conditions e overselling.
3. **Abuso de API** — bots ou usuários mal-intencionados disparando requisições em excesso (checkout, cupons, login).

O projeto endereça esses três problemas com soluções que usam Redis de forma idiomática, não apenas ilustrativa.

---

## Por que Redis para cada problema (teoria)

Esta seção existe para responder, para cada funcionalidade, a pergunta **"por que Redis e não só Postgres/memória da aplicação?"** — é o raciocínio que sustenta cada decisão técnica do projeto.

### 1. Cache de Catálogo (Cache-Aside Pattern)

**Problema:** produtos são lidos centenas de vezes mais do que são escritos. Bater no Postgres a cada `GET /produtos/{id}` desperdiça I/O de disco e conexões de um recurso que muda pouco.

**Por que Redis:** é uma estrutura de chave-valor em memória, então leituras são da ordem de microssegundos em vez de milissegundos. O padrão *cache-aside* (ler do cache; se não existir, buscar no banco e popular o cache) é o mais simples de raciocinar e o mais fácil de depurar quando algo sai errado — não exige que o Redis "saiba" sobre o banco.

**Ferramentas usadas:**
- `@Cacheable`, `@CacheEvict`, `@CachePut` (Spring Cache abstraction) para não escrever a lógica de cache-aside manualmente.
- TTL diferenciado por tipo de dado — produtos populares podem ter TTL mais curto (dado mais "quente", mais provável de mudar de preço/estoque) enquanto produtos nichados podem ter TTL mais longo.
- Serialização customizada com Jackson, porque a serialização padrão do Spring Cache pode falhar ao desserializar tipos genéricos ou polimórficos.

### 2. Gerenciamento de Sessão e Carrinho de Compras

**Problema:** se a sessão do usuário e o carrinho vivem só na memória de uma instância da aplicação, você não consegue escalar horizontalmente sem *sticky sessions* (o que complica load balancing e cria um ponto único de falha por instância).

**Por que Redis:** ao externalizar sessão e carrinho para um armazenamento compartilhado, qualquer instância da API pode atender qualquer requisição do usuário. Isso é essencial para escalar horizontalmente.

**Ferramentas usadas:**
- Carrinho como **Hash** (`HSET`/`HGETALL`) — um Hash mapeia naturalmente `produto_id → quantidade`, permitindo atualizar um item sem reescrever o carrinho inteiro (ao contrário de guardar um JSON serializado como string).
- TTL no carrinho, simulando "abandono de carrinho" — depois de X tempo sem atividade, ele expira.
- Spring Session + Redis para desacoplar sessão HTTP da instância da aplicação.

### 3. Rate Limiting de API

**Problema:** rotas sensíveis (login, checkout, aplicação de cupom) são alvos naturais de abuso — força bruta de senha, tentativa de adivinhar códigos de cupom, ou scripts tentando comprar itens em estoque limitado.

**Por que Redis:** `INCR` é atômico — múltiplas requisições concorrentes incrementando o mesmo contador não geram race condition, o que seria um risco real se o contador fosse mantido em memória da aplicação sob concorrência.

**Ferramentas usadas:**
- Algoritmo **fixed window counter**: `INCR` na chave (ex.: `rate:login:<ip>`) + `EXPIRE` para abrir a janela.
- Interceptor/filter do Spring aplicado nas rotas sensíveis.
- Header `429 Too Many Requests` com tempo de espera calculado a partir do TTL restante da chave (`TTL key`).

**Limitação conhecida (documentada aqui de propósito):** fixed window tem uma falha de borda — se a janela reinicia às 12:00:00, um usuário pode disparar N requisições às 11:59:59 e outras N às 12:00:01, efetivamente dobrando o limite em um intervalo de ~2 segundos. Isso é aceitável para o escopo de estudo deste projeto, mas é o motivo do item "sliding window com Lua" estar em Próximos Passos.

### 4. Lock Distribuído para Controle de Estoque

**Problema:** dois usuários podem tentar comprar o último item do estoque no mesmo instante. Sem controle de concorrência, ambas as transações podem ler `estoque = 1`, ambas decidirem confirmar, e o sistema faz *overselling* (vende mais do que tem).

**Por que Redis:** `SET key value NX EX` é atômico — a operação "criar a chave somente se ela não existir, com expiração" acontece em um único passo no Redis, garantindo que só um processo consiga "adquirir" o lock mesmo sob concorrência real.

**Ferramentas usadas:**
- `SET lock:produto:{id} <token-único> NX EX <segundos>` para adquirir o lock.
- Liberação do lock via **script Lua** que faz *compare-and-delete*: só apaga a chave se o valor armazenado for igual ao token que a instância gerou. Isso evita que o Processo B libere um lock que já pertence ao Processo C (cenário em que o Processo A demorou mais que o TTL e o lock expirou sozinho).

**Limitação conhecida:** essa abordagem (lock single-node) não é resiliente a falhas de um único nó Redis em cenários de alta criticidade — para isso existe o algoritmo Redlock (múltiplos nós Redis independentes). Para o escopo deste projeto (estudo, single-node), `SET NX EX` + script Lua de release é suficiente e é o padrão mais comum encontrado em produção para locks de baixa/média criticidade.

### 5. Pub/Sub para Eventos Assíncronos

**Problema:** quando um pedido é criado, várias ações precisam acontecer (enviar e-mail, notificar estoque baixo) mas elas não deveriam bloquear a resposta HTTP do checkout nem acoplar o serviço de pedidos ao serviço de notificação.

**Por que Redis:** Pub/Sub permite que o serviço de pedidos apenas publique um evento (`pedido.criado`) sem saber quem está ouvindo, e o worker consome de forma assíncrona. Simples de implementar para fins de estudo, embora não seja durável (mensagens não persistem se não houver subscriber ativo — ver observação abaixo).

**Ferramentas usadas:**
- Publicação de eventos de domínio (`pedido.criado`, `estoque.baixo`) em canais Redis.
- Serviço worker separado consumindo os eventos.

**Trade-off importante:** Redis Pub/Sub *não* garante entrega — se o worker estiver fora do ar no momento da publicação, o evento é perdido. Para um cenário de produção real, a alternativa seria Redis Streams (com consumer groups e replay) ou uma fila dedicada (RabbitMQ/Kafka). Isso é aceitável aqui porque o objetivo é aprender o padrão pub/sub, não construir um sistema de mensageria durável.

### 6. Ranking de Produtos em Tempo Real (Sorted Sets)

**Problema:** calcular "produtos mais vendidos da semana" via `SELECT ... GROUP BY ... ORDER BY ... COUNT(*)` no Postgres a cada requisição é caro, especialmente se o endpoint for consultado com frequência.

**Por que Redis:** um Sorted Set mantém os elementos já ordenados por score, então `ZREVRANGE` retorna o top-N em tempo constante/logarítmico, sem nenhuma agregação on-the-fly.

**Ferramentas usadas:**
- `ZINCRBY` para incrementar o score do produto (visualização ou venda) a cada evento.
- `ZREVRANGE` para servir o endpoint de "mais vendidos" direto do Redis.

**Origem do dado:** o Sorted Set é alimentado a partir de `itens_pedido` (cada item vendido gera um `ZINCRBY` no produto correspondente) — ou seja, o dado no Redis é *derivado* da tabela transacional, nunca a fonte de verdade.

### 7. Invalidação e Consistência de Cache

**Problema:** cache desatualizado (*stale data*) pode mostrar preço ou estoque errado para o usuário.

**Por que Redis + Spring Cache:** `@CacheEvict` no update/delete do produto garante que a próxima leitura force um cache miss e repopule com dado fresco — trocando "sempre consistente" por "consistente após a próxima escrita", que é aceitável para catálogo de e-commerce (não é um domínio financeiro onde inconsistência de milissegundos é crítica).

**Ferramentas usadas:**
- `@CacheEvict` na atualização/remoção de produtos.
- Monitoramento de hit/miss ratio via Actuator + Micrometer, para validar empiricamente se o TTL escolhido está gerando um cache eficaz.

---

## Modelagem do Banco de Dados (PostgreSQL)

Estas são as tabelas mínimas para exercitar todas as funcionalidades descritas acima. Elas representam a **fonte de verdade** — o Redis nunca é fonte de dados definitiva, apenas cache/estrutura de suporte em torno destas tabelas.

### `produtos`

Base para o cache-aside e para o ranking (Sorted Set é alimentado a partir do id desta tabela).

```sql
CREATE TABLE produtos (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    descricao TEXT,
    preco NUMERIC(10,2) NOT NULL,
    estoque INTEGER NOT NULL DEFAULT 0,
    categoria VARCHAR(80),
    criado_em TIMESTAMP NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT now()
);
```

### `usuarios`

Necessária para autenticação (rate limiting no login) e para vincular carrinho/pedidos.

```sql
CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);
```

### `pedidos`

Testa o fluxo completo de checkout, incluindo o lock distribuído sobre o estoque do produto.

```sql
CREATE TABLE pedidos (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE', -- PENDENTE, CONFIRMADO, CANCELADO
    total NUMERIC(10,2) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);
```

### `itens_pedido`

Detalha quais produtos e quantidades entraram em cada pedido — usado para validar se o lock impediu overselling, e alimenta o Sorted Set de ranking.

```sql
CREATE TABLE itens_pedido (
    id BIGSERIAL PRIMARY KEY,
    pedido_id BIGINT NOT NULL REFERENCES pedidos(id),
    produto_id BIGINT NOT NULL REFERENCES produtos(id),
    quantidade INTEGER NOT NULL,
    preco_unitario NUMERIC(10,2) NOT NULL
);
```

### `cupons`

Usada para testar o rate limiting em uma rota de alto risco de abuso (tentativas de força bruta de código de cupom).

```sql
CREATE TABLE cupons (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) UNIQUE NOT NULL,
    percentual_desconto NUMERIC(5,2) NOT NULL,
    valido_ate TIMESTAMP NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);
```

---

## O que fica só no Redis

Estruturas sem tabela equivalente no Postgres — isso é intencional e reforça na prática a diferença entre dado transacional e dado efêmero/derivado:

| Estrutura Redis | Tipo | Propósito |
|---|---|---|
| Carrinho de compras | Hash + TTL | Estado temporário, não confirmado |
| Sessão HTTP | Spring Session | Efêmero por natureza |
| Contadores de rate limiting | String (INCR/EXPIRE) | Só importa dentro da janela de tempo |
| Locks de checkout | String (SET NX EX) | Existe só durante a transação |
| Ranking de mais vendidos | Sorted Set | Derivado de `itens_pedido`, recalculável |

---

## Arquitetura

```
┌──────────┐      ┌─────────────────────┐      ┌──────────────┐
│ Cliente  │─────▶│  Spring Boot API    │─────▶│  PostgreSQL  │
└──────────┘      │  (REST + Session)   │      │ (dado fonte) │
                   └──────────┬──────────┘      └──────────────┘
                              │
                   ┌──────────▼──────────┐
                   │        Redis        │
                   │ Cache | Sessions    │
                   │ Rate Limit | Locks  │
                   │ Pub/Sub | Sorted Sets│
                   └──────────┬──────────┘
                              │
                   ┌──────────▼──────────┐
                   │   Worker Consumer   │
                   │    (notificações)   │
                   └──────────────────────┘
```

---

## Stack Técnica

- Java 17+ / Spring Boot 3
- Spring Data JPA (persistência relacional — PostgreSQL)
- Spring Data Redis (RedisTemplate, Lettuce como client)
- Spring Cache Abstraction
- Spring Session Data Redis
- Docker & Docker Compose (Redis, PostgreSQL, Redis Insight)
- Testcontainers (testes de integração com Redis real)
- Micrometer + Actuator (observabilidade de cache)

---

## Principais Endpoints

| Método | Rota | Redis aplicado |
|---|---|---|
| GET | `/produtos/{id}` | Cache-aside |
| POST | `/carrinho/{userId}/itens` | Hash + TTL |
| POST | `/checkout/{userId}` | Lock distribuído |
| POST | `/auth/login` | Rate limiting |
| GET | `/produtos/mais-vendidos` | Sorted Set |
| POST | `/cupons/aplicar` | Rate limiting |

---

## Roteiro de Implementação — Passo a Passo

Ordem sugerida, pensada para que cada etapa dependa apenas do que já foi construído antes.

### Etapa 0 — Infraestrutura base
- [ ] Criar `docker-compose.yml` com serviços: `postgres`, `redis`, `redis-insight`.
- [ ] Definir variáveis de ambiente (porta do Postgres, credenciais, porta do Redis) em `.env` ou `application.yml`.
- [ ] Subir os containers e validar conexão manual (`psql`, `redis-cli PING`).

### Etapa 1 — Modelo de dados e persistência
- [ ] Criar as 5 tabelas (`produtos`, `usuarios`, `pedidos`, `itens_pedido`, `cupons`) via migration (Flyway ou Liquibase — não SQL manual, para manter histórico versionado).
- [ ] Criar as entidades JPA correspondentes.
- [ ] Criar repositórios Spring Data JPA e popular a tabela `produtos` com dados de teste (seed).

### Etapa 2 — Conexão com Redis
- [ ] Configurar `RedisTemplate` com Lettuce e pool de conexões.
- [ ] Validar conexão com um teste simples (`SET`/`GET` manual via `RedisTemplate`).

### Etapa 3 — Cache-aside no catálogo
- [ ] Anotar `ProdutoService.buscarPorId` com `@Cacheable`.
- [ ] Anotar update/delete com `@CacheEvict`.
- [ ] Configurar TTL por tipo de cache no `application.yml`.
- [ ] Validar no Redis Insight: primeira leitura vai ao banco, segunda vem do cache.

### Etapa 4 — Sessão e carrinho de compras
- [ ] Configurar Spring Session + Redis.
- [ ] Implementar `CarrinhoService` usando `HSET`/`HGETALL` (Hash por `userId`).
- [ ] Aplicar TTL no carrinho.
- [ ] Testar expiração: item deve sumir do carrinho após o TTL.

### Etapa 5 — Rate limiting
- [ ] Implementar `RateLimiterService` com `INCR` + `EXPIRE` (fixed window).
- [ ] Criar interceptor/filter aplicado em `/auth/login`, `/checkout/**`, `/cupons/aplicar`.
- [ ] Retornar `429` com header indicando tempo de espera (via `TTL` da chave).
- [ ] Testar: disparar N+1 requisições na janela e validar o 429.

### Etapa 6 — Lock distribuído no checkout
- [ ] Implementar aquisição de lock com `SET lock:produto:{id} <token> NX EX <segundos>`.
- [ ] Implementar liberação via script Lua (compare-and-delete pelo token).
- [ ] Integrar o lock no fluxo de checkout, envolvendo a leitura/decremento de estoque.
- [ ] Escrever teste de concorrência determinístico (`ExecutorService` + `CountDownLatch` disparando duas requisições simultâneas no último item em estoque).

### Etapa 7 — Pub/Sub
- [ ] Publicar evento `pedido.criado` ao confirmar um pedido.
- [ ] Publicar evento `estoque.baixo` quando o estoque cruzar um limiar.
- [ ] Criar serviço worker separado (pode ser outro `@Service` com listener, ou módulo standalone) que consome os eventos e loga/simula envio de notificação.

### Etapa 8 — Ranking com Sorted Sets
- [ ] No fluxo de checkout confirmado, disparar `ZINCRBY` por item vendido.
- [ ] Implementar endpoint `/produtos/mais-vendidos` usando `ZREVRANGE`, sem tocar no Postgres.
- [ ] Validar que o ranking reflete a ordem correta após simular vendas.

### Etapa 9 — Observabilidade
- [ ] Expor métricas de cache (hit/miss ratio) via Actuator + Micrometer.
- [ ] Validar no endpoint `/actuator/metrics` os contadores de cache.

### Etapa 10 — Testes de integração
- [ ] Configurar Testcontainers para Redis e Postgres reais nos testes.
- [ ] Cobrir: cache-aside, expiração de carrinho, rate limiting, lock sob concorrência, pub/sub, ranking.

---

## Roteiro de Testes por Funcionalidade

| Funcionalidade | Tabelas envolvidas | O que validar |
|---|---|---|
| Cache-aside | `produtos` | Primeira leitura vai ao banco; leituras seguintes vêm do Redis (checar no Redis Insight) |
| Carrinho de compras | `produtos`, `usuarios` | Item some do carrinho após o TTL expirar |
| Checkout + lock distribuído | `produtos`, `pedidos`, `itens_pedido` | Duas requisições simultâneas no último item em estoque — só uma deve confirmar o pedido |
| Rate limiting | `usuarios`, `cupons` | Após N tentativas na janela de tempo, API deve responder 429 |
| Pub/Sub | `pedidos` | Ao criar um pedido, o worker consumidor deve logar/processar o evento |
| Ranking (Sorted Set) | `produtos`, `itens_pedido` | Após simular vendas, o endpoint de "mais vendidos" deve refletir a ordem correta sem consultar o Postgres |
| Auditoria de cache | `produtos` | Atualizar um produto deve invalidar a entrada correspondente no Redis |

---

## Como Executar

```bash
git clone <link-do-repositorio>
cd shop-flow
docker-compose up -d      # sobe Redis, PostgreSQL e Redis Insight
./mvnw spring-boot:run
```

Redis Insight disponível em `http://localhost:8001` para inspecionar chaves, TTLs e estruturas em tempo real durante os testes.

---

## Próximos Passos (Evolução do Projeto)

- Migrar rate limiter para o algoritmo sliding window usando Lua scripting.
- Avaliar Redisson para locks distribuídos mais robustos (Redlock).
- Adicionar cache de segundo nível para relatórios agregados.
- Avaliar Redis Streams como alternativa mais durável ao Pub/Sub atual.
- Adicionar métricas de taxa de sucesso/falha de lock (concorrência vs. estoque real zerado).

---

*Projeto construído com fins de estudo e portfólio, aplicando padrões de uso de Redis reconhecidos no mercado para sistemas de e-commerce de alta concorrência.*
