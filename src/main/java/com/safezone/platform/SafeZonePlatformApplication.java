package com.safezone.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SafeZonePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(SafeZonePlatformApplication.class, args);
    }
}
