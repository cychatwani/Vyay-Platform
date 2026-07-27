package com.vyay.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@SpringBootApplication
@ConfigurationPropertiesScan
    public class CoreApplication {

    public static void main(String[] args) {
        System.out.println("Spring app started now ... ");
        SpringApplication.run(CoreApplication.class, args);
    }


}

