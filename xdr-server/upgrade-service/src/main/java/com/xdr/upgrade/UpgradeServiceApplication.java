package com.xdr.upgrade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.xdr.upgrade", "com.xdr.common"})
public class UpgradeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UpgradeServiceApplication.class, args);
    }
}
