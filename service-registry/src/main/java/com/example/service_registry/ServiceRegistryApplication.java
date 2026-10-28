package com.example.service_registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * SERVICE REGISTRY (Eureka Server).
 *
 * What it does: every microservice (gateway, order, payment, inventory) tells this server
 * "I am ORDER-SERVICE and I live at 10.0.0.5:8080". Other services ask it where to find someone.
 *
 * IMPORTANT: Eureka is a side-channel (a phone book). Business requests NEVER flow through it.
 */
@SpringBootApplication      // Turns on Spring Boot auto-configuration + component scanning for this package.
@EnableEurekaServer         // Makes this application behave as a Eureka server (adds the dashboard at /).
public class ServiceRegistryApplication {

    public static void main(String[] args) {
        // Starts the embedded web server and the Spring application context.
        SpringApplication.run(ServiceRegistryApplication.class, args);
    }
}
