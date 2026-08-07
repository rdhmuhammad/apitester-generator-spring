package com.apitester.generator;

import com.apitester.generator.annotation.EnableApiTester;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableApiTester
public class ApitesterGeneratorSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApitesterGeneratorSpringApplication.class, args);
    }
}
