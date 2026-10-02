package com.example.todobackend.project;

import com.example.todobackend.todo.TodoRepository;
import com.example.todobackend.user.AppUserRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/projects")
@Transactional
public class ProjectController {
  private final ProjectRepository projects;
  private final AppUserRepository users;
  private final TodoRepository tasks;

  public record ProjectInput(String name, String color) {}

  public record ProjectView(Long id, String name, String color) {}

  public ProjectController(
      ProjectRepository projects, AppUserRepository users, TodoRepository tasks) {
    this.projects = projects;
    this.users = users;
    this.tasks = tasks;
  }

  @GetMapping
  public List<ProjectView> list(Authentication auth) {
    return projects.findByUserUsernameOrderByNameAsc(auth.getName()).stream()
        .map(this::view)
        .toList();
  }

  @PostMapping
  public ResponseEntity<ProjectView> create(Authentication auth, @RequestBody ProjectInput input) {
    validate(input);
    if (projects.findByUserUsernameOrderByNameAsc(auth.getName()).size() >= 100)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "You can create up to 100 projects");
    if (projects.existsByNameAndUserUsername(input.name().trim(), auth.getName())) conflict();
    return ResponseEntity.status(201)
        .body(
            view(
                projects.saveAndFlush(
                    new Project(
                        users.findByUsername(auth.getName()).orElseThrow(),
                        input.name().trim(),
                        input.color()))));
  }

  @PutMapping("/{id}")
  public ProjectView update(
      Authentication auth, @PathVariable Long id, @RequestBody ProjectInput input) {
    validate(input);
    Project p = owned(id, auth.getName());
    String old = p.getName();
    if (!old.equals(input.name().trim())
        && projects.existsByNameAndUserUsername(input.name().trim(), auth.getName())) conflict();
    p.setName(input.name().trim());
    p.setColor(input.color());
    tasks
        .findByUserId(users.findByUsername(auth.getName()).orElseThrow().getId(), Sort.unsorted())
        .stream()
        .filter(t -> t.getProject().equals(old))
        .forEach(t -> t.setProject(p.getName()));
    return view(p);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(Authentication auth, @PathVariable Long id) {
    Project p = owned(id, auth.getName());
    tasks
        .findByUserId(users.findByUsername(auth.getName()).orElseThrow().getId(), Sort.unsorted())
        .stream()
        .filter(t -> t.getProject().equals(p.getName()))
        .forEach(t -> t.setProject(""));
    projects.delete(p);
    return ResponseEntity.noContent().build();
  }

  private Project owned(Long id, String username) {
    return projects
        .findByIdAndUserUsername(id, username)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project not found"));
  }

  private ProjectView view(Project p) {
    return new ProjectView(p.getId(), p.getName(), p.getColor());
  }

  private void conflict() {
    throw new ResponseStatusException(
        HttpStatus.CONFLICT, "A project with this name already exists");
  }

  private void validate(ProjectInput input) {
    if (input.name() == null || input.name().isBlank() || input.name().trim().length() > 80)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Project name must contain 1–80 characters");
    if (input.color() == null || !input.color().matches("#[0-9a-fA-F]{6}"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a valid project color");
  }
}
