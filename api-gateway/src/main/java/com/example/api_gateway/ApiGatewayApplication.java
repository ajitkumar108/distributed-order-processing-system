package com.example.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API GATEWAY: the only door the outside world uses.
 *
 * Client -> http://localhost:9090/orders  -> gateway looks up ORDER-SERVICE in Eureka -> forwards the request.
 * The routing rules live in application.yml (no Java needed for simple routes).
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
