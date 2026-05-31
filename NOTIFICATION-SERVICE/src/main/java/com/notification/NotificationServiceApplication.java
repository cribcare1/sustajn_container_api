package com.notification;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync
@EnableFeignClients(basePackages = "com.notification")
public class NotificationServiceApplication {
	@PostConstruct
	public void init(){
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Dubai"));
	}


	public static void main(String[] args) {
		SpringApplication.run(NotificationServiceApplication.class, args);
	}

}
