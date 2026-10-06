package com.example.distributed_order_processing_system.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;


@RestController 
public class HomeController {
    @GetMapping("/")
    public String getMethodName() {
        return "Welcome to Home Page";
    }
    
}
