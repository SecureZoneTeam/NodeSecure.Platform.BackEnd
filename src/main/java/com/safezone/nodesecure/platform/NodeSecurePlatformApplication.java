package com.safezone.nodesecure.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class NodeSecurePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(NodeSecurePlatformApplication.class, args);
    }

}
