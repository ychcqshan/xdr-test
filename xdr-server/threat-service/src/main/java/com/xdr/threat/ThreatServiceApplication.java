package com.xdr.threat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.xdr.threat", "com.xdr.common"})
public class ThreatServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ThreatServiceApplication.class, args);
    }
}
