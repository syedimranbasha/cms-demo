package com.walmart.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.walmart.cms")
public class ConfigurationManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigurationManagementApplication.class, args);
    }
}
