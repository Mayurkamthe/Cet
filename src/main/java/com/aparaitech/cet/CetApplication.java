package com.aparaitech.cet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.aparaitech.cet.config.InstituteProperties;

@SpringBootApplication
@EnableConfigurationProperties(InstituteProperties.class)
public class CetApplication {
    public static void main(String[] args) {
        // Create data directory for SQLite
        new java.io.File("data").mkdirs();
        SpringApplication.run(CetApplication.class, args);
    }
}
