package com.example.oa.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = "com.example.oa")
@SpringBootApplication
public class OaPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(OaPortalApplication.class, args);
    }
}
