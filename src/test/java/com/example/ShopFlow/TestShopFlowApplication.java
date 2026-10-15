package com.example.ShopFlow;

import org.springframework.boot.SpringApplication;

public class TestShopFlowApplication {

	public static void main(String[] args) {
		SpringApplication.from(ShopFlowApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
