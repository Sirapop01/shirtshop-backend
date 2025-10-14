package com.shirtshop;

import com.shirtshop.entity.User;
import com.shirtshop.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Set;


@SpringBootApplication
@EnableScheduling
public class ShirtShopBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(ShirtShopBackendApplication.class, args);
	}


}
