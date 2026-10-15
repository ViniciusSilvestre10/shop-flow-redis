package com.example.ShopFlow.config;

import org.springframework.stereotype.Component;

@Component
public class NotificationWorker {

    public void handlePedidoCriado(String message) {
        System.out.println("----------------------------------------");
        System.out.println("[WORKER PUB/SUB] Evento recebido: pedido.criado");
        System.out.println("Payload: " + message);
        System.out.println("Ação: Simulando envio de e-mail de confirmação...");
        System.out.println("----------------------------------------");
    }

    public void handleEstoqueBaixo(String message) {
        System.out.println("----------------------------------------");
        System.out.println("[WORKER PUB/SUB] Alerta recebido: estoque.baixo");
        System.out.println("Payload: " + message);
        System.out.println("Ação: Notificando setor de reposição...");
        System.out.println("----------------------------------------");
    }
}
