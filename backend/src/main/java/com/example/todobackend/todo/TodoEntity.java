package com.example.todobackend.todo;

import com.example.todobackend.user.AppUser;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "todo", indexes = @Index(name = "idx_todo_user", columnList = "user_id"))
public class TodoEntity {
  @Column(name = "recurrence_generated", nullable = false)
  private boolean recurrenceGenerated;

  public boolean isRecurrenceGenerated() {
    return recurrenceGenerated;
  }

  public void markRecurrenceGenerated() {
    recurrenceGenerated = true;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(nullable = false, length = 255)
  private String description;

  @Column(name = "target_date")
  private LocalDate targetDate;

  @Column(nullable = false)
  private boolean done;

  @Column(length = 4000, nullable = false)
  private String notes = "";

  @Column(length = 10, nullable = false)
  private String priority = "MEDIUM";

  @Column(length = 80, nullable = false)
  private String project = "";

  @Column(length = 500, nullable = false)
  private String tags = "";

  @Column(length = 10, nullable = false)
  private String recurrence = "NONE";

  @Column(nullable = false)
  private boolean starred;

  @Column(name = "subtasks_json", length = 16000, nullable = false)
  private String subtasksJson = "[]";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @Version
  @Column(nullable = false)
  private Long version = 0L;

  protected TodoEntity() {}

  public TodoEntity(AppUser user, String description, LocalDate targetDate, boolean done) {
    this.user = user;
    this.description = description;
    this.targetDate = targetDate;
    this.done = done;
    if (done) completedAt = Instant.now();
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
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

  public void setDescription(String v) {
    description = v;
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  public void setTargetDate(LocalDate v) {
    targetDate = v;
  }

  public boolean isDone() {
    return done;
  }

  public void setDone(boolean v) {
    done = v;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String v) {
    notes = v;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String v) {
    priority = v;
  }

  public String getProject() {
    return project;
  }

  public void setProject(String v) {
    project = v;
  }

  public String getTags() {
    return tags;
  }

  public void setTags(String v) {
    tags = v;
  }

  public String getRecurrence() {
    return recurrence;
  }

  public void setRecurrence(String v) {
    recurrence = v;
  }

  public boolean isStarred() {
    return starred;
  }

  public void setStarred(boolean v) {
    starred = v;
  }

  public String getSubtasksJson() {
    return subtasksJson;
  }

  public void setSubtasksJson(String v) {
    subtasksJson = v;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant v) {
    completedAt = v;
  }

  public Instant getDeletedAt() {
    return deletedAt;
  }

  public void setDeletedAt(Instant v) {
    deletedAt = v;
  }

  public Long getVersion() {
    return version;
  }
}
