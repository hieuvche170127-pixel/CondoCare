package com.swp391.condocare_swp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CondoCareSwpApplication {

    public static void main(String[] args) {
        SpringApplication.run(CondoCareSwpApplication.class, args);
    }

}
