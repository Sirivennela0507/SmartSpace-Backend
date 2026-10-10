package com.smartspace.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    
    @GetMapping("/")
    public String home() {
        return "SmartSpace Backend is Working!";
    }
    
    @GetMapping("/api/test")
    public String test() {
        return "Test API Working!";
    }
}