package com.smartspace.backend.controller;

import com.smartspace.backend.dto.RegisterRequest;
import com.smartspace.backend.model.User;
import com.smartspace.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest req) {
        return authService.register(req.name, req.email, req.password);
    }
}