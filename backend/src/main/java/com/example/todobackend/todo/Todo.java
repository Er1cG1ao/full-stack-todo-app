package com.example.todobackend.todo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class Todo {
  public record Subtask(String id, String title, boolean done) {}

  private Long id;
  private String username;
  private String description;
  private LocalDate targetDate;
  private boolean done;
  private String notes = "";
  private String priority = "MEDIUM";
  private String project = "";
  private String tags = "";
  private String recurrence = "NONE";
  private boolean starred;
  private List<Subtask> subtasks = List.of();
  private Instant createdAt;
  private Instant updatedAt;
  private Instant completedAt;
  private Instant deletedAt;
  private Long version;

  public Todo() {}

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

  public void setId(Long value) {
    id = value;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String value) {
    username = value;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String value) {
    description = value;
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  public void setTargetDate(LocalDate value) {
    targetDate = value;
  }

  public boolean isDone() {
    return done;
  }

  public void setDone(boolean value) {
    done = value;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String value) {
    notes = value;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String value) {
    priority = value;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String value) {
    project = value;
  }

  public String getTags() {
    return tags;
  }

  public void setTags(String value) {
    tags = value;
  }

  public String getRecurrence() {
    return recurrence;
  }

  public void setRecurrence(String value) {
    recurrence = value;
  }

  public boolean isStarred() {
    return starred;
  }

  public void setStarred(boolean value) {
    starred = value;
  }

  public List<Subtask> getSubtasks() {
    return subtasks;
  }

  public void setSubtasks(List<Subtask> value) {
    subtasks = value;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant value) {
    createdAt = value;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant value) {
    updatedAt = value;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant value) {
    completedAt = value;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(Instant value) {
    deletedAt = value;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long value) {
    version = value;
  }
}
