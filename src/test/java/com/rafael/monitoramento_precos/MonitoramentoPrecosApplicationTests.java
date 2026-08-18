package com.rafael.monitoramento_precos;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:postgresql://localhost:5433/monitoramento_db",
		"spring.datasource.username=postgres",
		"spring.datasource.password=admin",
		"spring.data.mongodb.uri=mongodb://localhost:27018/monitoramento_db",
		"api.security.token.secret=chave-falsa-jwt",
		"api.security.pepper=pimenta-falsa-pepper",
		"api.scraperapi.key=chave-falsa-scraperapi-para-testes"
})
class MonitoramentoPrecosApplicationTests {

	@Test
	void contextLoads() {
	}

}