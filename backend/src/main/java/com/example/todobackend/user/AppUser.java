package com.example.todobackend.user;

import com.example.todobackend.todo.TodoEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * ★ lesson8 新增文件 1 / 8。
 *
 *
 *   CREATE TABLE app_user (
 *     id       BIGINT AUTO_INCREMENT PRIMARY KEY,   ← @Id + @GeneratedValue(IDENTITY)
 *     username VARCHAR(50) NOT NULL UNIQUE          ← @Column(nullable/unique/length)
 *   );
 *
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @OneToMany(mappedBy = "user")
    private List<TodoEntity> todos = new ArrayList<>();

    protected AppUser() {
    }

    public AppUser(String username) {
        this.username = username;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<TodoEntity> getTodos() {
        return todos;
    }

    @Override
    public String toString() {
        return "AppUser{id=%d, username='%s'}".formatted(id, username);
    }
}
