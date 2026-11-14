package com.university.ums;

import com.university.ums.config.DataSeeder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class UmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(UmsApplication.class, args);
    }

    @Bean
    CommandLineRunner seedDatabase(DataSeeder dataSeeder) {
        return args -> dataSeeder.seed();
    }
}
