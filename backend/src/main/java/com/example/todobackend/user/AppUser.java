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
 * <p>CREATE TABLE app_user ( id BIGINT AUTO_INCREMENT PRIMARY KEY, ← @Id
 * + @GeneratedValue(IDENTITY) username VARCHAR(50) NOT NULL UNIQUE
 * ← @Column(nullable/unique/length) );
 */
@Entity
@Table(name = "app_user")
public class AppUser {
  @jakarta.persistence.Version
  @Column(name = "row_version", nullable = false)
  private Long rowVersion = 0L;

  @Column(name = "recovery_hash", length = 64)
  private String recoveryHash;

  public String getRecoveryHash() {
    return recoveryHash;
  }

  public void setRecoveryHash(String value) {
    recoveryHash = value;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 50)
  private String username;

  // Null only for legacy accounts, which cannot authenticate until migrated.
  @Column(name = "password_hash", length = 100)
  private String passwordHash;

  @Column(name = "credential_version", nullable = false)
  private long credentialVersion;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @OneToMany(mappedBy = "user")
  private List<TodoEntity> todos = new ArrayList<>();

  protected AppUser() {}

  public AppUser(String username) {
    this.username = username;
    this.createdAt = Instant.now();
  }

  public Long getId() {
    return id;
  }

  public AppUser(String username, String passwordHash) {
    this(username);
    this.passwordHash = passwordHash;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public long getCredentialVersion() {
    return credentialVersion;
  }

  public void changePassword(String hash) {
    passwordHash = hash;
    credentialVersion++;
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
