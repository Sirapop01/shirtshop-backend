package com.shirtshop;

import com.shirtshop.entity.User;
import com.shirtshop.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.util.Set;


@SpringBootApplication
public class ShirtShopBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(ShirtShopBackendApplication.class, args);
	}


}
