package com.inventory;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.util.TimeZone;

@SpringBootApplication
@EnableFeignClients
@EnableDiscoveryClient
public class InventoryServiceApplication {
	@PostConstruct
	public void init(){
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Dubai"));
	}


	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}
}
