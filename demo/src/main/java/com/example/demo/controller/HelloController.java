package com.example.demo.controller;

import com.example.demo.dto.GreetRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/hello")
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("Hello from Spring Boot!");
    }

    @PostMapping("/greet")
    public ResponseEntity<String> greet(@Valid @RequestBody GreetRequest req) {
        return ResponseEntity.ok("Hello, " + req.getName() + "!");
    }
}