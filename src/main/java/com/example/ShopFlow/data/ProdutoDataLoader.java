package com.example.ShopFlow.data;

import com.example.ShopFlow.domain.Produto;
import com.example.ShopFlow.domain.Usuario;
import com.example.ShopFlow.repository.ProdutoRepository;
import com.example.ShopFlow.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@Profile("!prod")
public class ProdutoDataLoader implements CommandLineRunner {

    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;

    public ProdutoDataLoader(ProdutoRepository produtoRepository, UsuarioRepository usuarioRepository) {
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public void run(String... args) {
        if (produtoRepository.count() == 0) {
            List<Produto> produtos = List.of(
                    criarProduto("Teclado Mecânico RGB", "Switch Blue com iluminação customizável", new BigDecimal("299.90"), 50, "Periféricos"),
                    criarProduto("Mouse Sem Fio Ergonômico", "Sensor 16.000 DPI e bateria recarregável", new BigDecimal("149.50"), 80, "Periféricos"),
                    criarProduto("Monitor Gamer 27''", "Painel IPS 144Hz 1ms Full HD", new BigDecimal("1299.00"), 20, "Monitores"),
                    criarProduto("Headset Surround 7.1", "Cancelamento de ruído passivo e cabo trançado", new BigDecimal("349.90"), 40, "Áudio"),
                    criarProduto("Cadeira Gamer Ergonomica", "Reclinável até 180° com apoio lombar", new BigDecimal("899.00"), 15, "Móveis"),
                    criarProduto("SSD NVMe 1TB M.2", "Leitura de 3500MB/s e escrita de 3000MB/s", new BigDecimal("450.00"), 60, "Hardware"),
                    criarProduto("Memória RAM 16GB DDR4", "3200MHz com dissipador de calor RGB", new BigDecimal("280.00"), 100, "Hardware"),
                    criarProduto("Webcam 1080p Full HD", "Foco automático e microfone estéreo", new BigDecimal("199.90"), 35, "Acessórios")
            );

            produtoRepository.saveAll(produtos);
            System.out.println(">>> [ShopFlow] 8 produtos inseridos com sucesso na base!");

            if (usuarioRepository.count() == 0) {
                Usuario usuario = new Usuario();
                usuario.setNome("Vinicius Silvestre");
                usuario.setEmail("vinicius@shopflow.com");
                usuario.setSenhaHash("123456"); // Senha simulada para testes

                usuarioRepository.save(usuario);
                System.out.println(">>> [ShopFlow] Usuário de teste criado com ID 1 com sucesso!");
            }
        }
    }

    private Produto criarProduto(String nome, String descricao, BigDecimal preco, Integer estoque, String categoria) {
        Produto p = new Produto();
        p.setNome(nome);
        p.setDescricao(descricao);
        p.setPreco(preco);
        p.setEstoque(estoque);
        p.setCategoria(categoria);
        return p;
    }
}