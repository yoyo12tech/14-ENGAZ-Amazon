package com.team14.amazon.shipping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import com.team14.amazon.contracts.feign.OrderServiceClient;

@SpringBootApplication
@EnableFeignClients(basePackages = {"com.team14.amazon.shipping", "com.team14.amazon.contracts.feign"})
@EntityScan("com.team14.amazon.shipping.model")
@EnableCaching
public class ShippingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShippingServiceApplication.class, args);
    }

}
