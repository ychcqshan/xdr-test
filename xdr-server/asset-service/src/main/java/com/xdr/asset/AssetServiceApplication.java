package com.xdr.asset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan(basePackages = {"com.xdr.asset", "com.xdr.common"})
@EnableScheduling
public class AssetServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssetServiceApplication.class, args);
    }
}
