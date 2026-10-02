package com.example.todobackend.project;

import com.example.todobackend.user.AppUser;
import jakarta.persistence.*;

@Entity
@Table(
    name = "project",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_project_user_name",
            columnNames = {"user_id", "name"}))
public class Project {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(nullable = false, length = 80)
  private String name;

  @Column(nullable = false, length = 7)
  private String color;

  protected Project() {}

  public Project(AppUser user, String name, String color) {
    this.user = user;
    this.name = name;
    this.color = color;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getColor() {
    return color;
  }

  public void setName(String v) {
    name = v;
  }

  public void setColor(String v) {
    color = v;
  }
}
