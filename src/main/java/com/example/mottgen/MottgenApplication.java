package com.example.mottgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MottgenApplication {

	// Rakenduse sisenemispunkt. Käivitab Spring Boot'i,
	// mis skaneerib komponendid ja avab veebiserveri.
	public static void main(String[] args) {
		SpringApplication.run(MottgenApplication.class, args);
	}

}
