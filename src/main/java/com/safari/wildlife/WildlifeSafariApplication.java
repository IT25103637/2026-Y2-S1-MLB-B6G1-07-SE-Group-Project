package com.safari.wildlife;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WildlifeSafariApplication {

    public static void main(String[] args) {
        SpringApplication.run(WildlifeSafariApplication.class, args);
    }
}
