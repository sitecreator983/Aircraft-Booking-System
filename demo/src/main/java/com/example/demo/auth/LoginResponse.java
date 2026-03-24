package com.example.demo.auth;

public class LoginResponse {
    public String username;
    public String message;

    public LoginResponse(String username, String message) {
        this.username = username;
        this.message = message;
    }
}