package com.rafael.monitoramento_precos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class MonitoramentoPrecosApplication {

	public static void main(String[] args) {
		SpringApplication.run(MonitoramentoPrecosApplication.class, args);
	}

}
