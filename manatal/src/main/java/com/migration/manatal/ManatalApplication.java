package com.migration.manatal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.migration.manatal.security.SecurityProperties;

@SpringBootApplication
@EnableConfigurationProperties(SecurityProperties.class)
public class ManatalApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManatalApplication.class, args);
	}

}
