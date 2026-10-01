package com.example.todobackend.todo;

import java.time.LocalDate;

/**
 * The backend Todo model. It has to line up exactly with the frontend's
 * lesson4/todo-list-with-backend/src/app/model/todo.ts:
 *
 *   export interface Todo {
 *     id: number;
 *     username?: string;
 *     description: string;
 *     targetDate: string;   // "2026-08-15"
 *     done: boolean;
 *   }
 */
public class Todo {

    private Long id;
    private String username;
    private String description;
    private LocalDate targetDate;
    private boolean done;

    // Step 3-①: Jackson uses this constructor when deserializing POST/PUT JSON.
    public Todo() {
    }


    public Todo(Long id, String username, String description, LocalDate targetDate, boolean done) {
        this.id = id;
        this.username = username;
        this.description = description;
        this.targetDate = targetDate;
        this.done = done;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    // Step 3-②: the JavaBean boolean getter keeps the JSON field name as "done".
    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    @Override
    public String toString() {
        return "Todo{id=%d, username='%s', description='%s', targetDate=%s, done=%s}"
                .formatted(id, username, description, targetDate, done);
    }
}
