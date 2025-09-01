package kr.co.hdi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class HdiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HdiApplication.class, args);
	}

}
