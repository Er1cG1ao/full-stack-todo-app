package com.example.todobackend.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String username) {
        super("No user named %s exists".formatted(username));
    }
}
