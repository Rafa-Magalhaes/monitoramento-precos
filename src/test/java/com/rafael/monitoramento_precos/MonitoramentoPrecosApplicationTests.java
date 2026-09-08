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
		"api.whatsapp.cloud.url=https://graph.facebook.com/v19.0",
		"api.whatsapp.cloud.phone-number-id=123456789",
		"api.whatsapp.cloud.access-token=token_falso_ci",
		// Novas propriedades mockadas para o teste passar
		"api.brightdata.host=host-falso",
		"api.brightdata.port=0000",
		"api.brightdata.username=user-falso",
		"api.brightdata.password=pass-falsa"
})
class MonitoramentoPrecosApplicationTests {

	@Test
	void contextLoads() {
	}

}