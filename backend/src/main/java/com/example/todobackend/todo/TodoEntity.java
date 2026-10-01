package com.example.todobackend.todo;

import com.example.todobackend.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 *
 *   CREATE TABLE todo (
 *     id          BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     user_id     BIGINT NOT NULL,
 *     description VARCHAR(255) NOT NULL,
 *     target_date DATE,
 *     done        BOOLEAN NOT NULL DEFAULT FALSE,
 *     CONSTRAINT fk_todo_user FOREIGN KEY (user_id) REFERENCES app_user(id)
 *   );
 *   */
 @Entity
 @Table(name = "todo")
 public class TodoEntity {

 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @ManyToOne(fetch = FetchType.LAZY, optional = false)
 @JoinColumn(name = "user_id", nullable = false)
 private AppUser user;

 @Column(nullable = false, length = 255)
 private String description;

 @Column(name = "target_date", nullable = false)
 private LocalDate targetDate;

 @Column(nullable = false)
 private boolean done;

 protected TodoEntity() {
 }

 public TodoEntity(AppUser user, String description, LocalDate targetDate, boolean done) {
 this.user = user;
 this.description = description;
 this.targetDate = targetDate;
 this.done = done;
 }

 public Long getId() {
 return id;
 }

 public AppUser getUser() {
 return user;
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

 public boolean isDone() {
 return done;
 }

 public void setDone(boolean done) {
 this.done = done;
 }
 }
