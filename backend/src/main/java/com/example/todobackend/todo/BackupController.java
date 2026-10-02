package com.example.todobackend.todo;

import com.example.todobackend.project.*;
import com.example.todobackend.user.AppUserRepository;
import java.time.Instant;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/backup")
public class BackupController {
  public record ProjectBackup(String name, String color) {}

  public record Backup(
      int formatVersion,
      Instant exportedAt,
      String username,
      List<Todo> tasks,
      List<ProjectBackup> projects) {}

  private final TodoJpaService tasks;
  private final ProjectRepository projects;
  private final AppUserRepository users;

  public BackupController(
      TodoJpaService tasks, ProjectRepository projects, AppUserRepository users) {
    this.tasks = tasks;
    this.projects = projects;
    this.users = users;
  }

  @GetMapping
  public Backup export(Authentication auth) {
    List<Todo> all = new ArrayList<>(tasks.list(auth.getName(), false));
    all.addAll(tasks.list(auth.getName(), true));
    return new Backup(
        1,
        Instant.now(),
        auth.getName(),
        all,
        projects.findByUserUsernameOrderByNameAsc(auth.getName()).stream()
            .map(p -> new ProjectBackup(p.getName(), p.getColor()))
            .toList());
  }

  @PostMapping("/import")
  @Transactional
  public Map<String, Integer> importBackup(Authentication auth, @RequestBody Backup backup) {
    if (backup.formatVersion() != 1
        || backup.tasks() == null
        || backup.tasks().size() > 500
        || backup.projects() == null
        || backup.projects().size() > 100)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Use a version 1 backup with up to 500 tasks and 100 projects");
    var user = users.findByUsername(auth.getName()).orElseThrow();
    int added = 0;
    var existing =
        new HashSet<>(
            projects.findByUserUsernameOrderByNameAsc(auth.getName()).stream()
                .map(Project::getName)
                .toList());
    for (ProjectBackup p : backup.projects()) {
      if (p == null
          || p.name() == null
          || p.name().isBlank()
          || p.name().length() > 80
          || p.color() == null
          || !p.color().matches("#[a-fA-F0-9]{6}"))
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Backup contains an invalid project");
      if (existing.add(p.name())) {
        if (existing.size() > 100)
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project limit exceeded");
        projects.save(new Project(user, p.name(), p.color()));
        added++;
      }
    }
    for (Todo t : backup.tasks()) {
      Todo saved = tasks.create(auth.getName(), t);
      if (t.getDeletedAt() != null) tasks.deleteById(auth.getName(), saved.getId());
    }
    return Map.of("tasks", backup.tasks().size(), "projects", added);
  }
}
