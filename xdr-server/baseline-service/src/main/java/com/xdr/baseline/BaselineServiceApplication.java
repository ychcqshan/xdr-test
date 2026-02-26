package com.xdr.baseline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.xdr.baseline", "com.xdr.common"})
@EnableScheduling
public class BaselineServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BaselineServiceApplication.class, args);
    }
}
