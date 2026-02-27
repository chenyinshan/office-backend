package com.example.oa.workflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = "com.example.oa")
@SpringBootApplication
public class OaWorkflowServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OaWorkflowServiceApplication.class, args);
    }
}
