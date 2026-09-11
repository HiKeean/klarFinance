package com.api.klarfinance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ConfigurationPropertiesScan
@EnableCaching
@EnableJpaRepositories
@SpringBootApplication
public class KlarFinanceApplication {

	public static void main(String[] args) {
		SpringApplication.run(KlarFinanceApplication.class, args);
	}

}
