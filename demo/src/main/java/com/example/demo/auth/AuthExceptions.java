package com.example.demo.auth;

public class AuthExceptions {
    public static class Unauthorized extends RuntimeException {
        public Unauthorized(String msg) { super(msg); }
    }

    public static class Locked extends RuntimeException {
        public Locked(String msg) { super(msg); }
    }
}
