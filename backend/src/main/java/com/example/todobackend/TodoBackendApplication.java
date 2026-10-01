package com.example.todobackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * lesson6: a full set of Todo CRUD endpoints for the lesson4 Angular frontend.
 *
 * Data still lives in memory (restart and it's gone) — swapping in a real database is lesson7.
 */
@SpringBootApplication
public class TodoBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TodoBackendApplication.class, args);
    }
}
