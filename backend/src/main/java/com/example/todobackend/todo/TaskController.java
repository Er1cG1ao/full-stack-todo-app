package com.example.todobackend.todo;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
  private final TodoJpaService tasks;

  public TaskController(TodoJpaService tasks) {
    this.tasks = tasks;
  }

  public record BulkRequest(List<Long> ids, String action) {}

  @GetMapping
  public List<Todo> list(Authentication user, @RequestParam(defaultValue = "false") boolean trash) {
    return tasks.list(user.getName(), trash);
  }

  @GetMapping("/{id}")
  public Todo get(Authentication user, @PathVariable long id) {
    return tasks.findById(user.getName(), id);
  }

  @PostMapping
  public ResponseEntity<Todo> create(Authentication user, @RequestBody Todo task) {
    Todo saved = tasks.create(user.getName(), task);
    return ResponseEntity.created(URI.create("/api/tasks/" + saved.getId())).body(saved);
  }

  @PutMapping("/{id}")
  public Todo update(Authentication user, @PathVariable long id, @RequestBody Todo task) {
    if (task.getVersion() == null)
      throw new com.example.todobackend.exception.InvalidTodoException("Task version is required");
    return tasks.update(user.getName(), id, task);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> trash(Authentication user, @PathVariable long id) {
    tasks.deleteById(user.getName(), id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/restore")
  public Todo restore(Authentication user, @PathVariable long id) {
    return tasks.restore(user.getName(), id);
  }

  @DeleteMapping("/{id}/permanent")
  public ResponseEntity<Void> purge(Authentication user, @PathVariable long id) {
    tasks.permanentlyDelete(user.getName(), id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/bulk")
  public List<Todo> bulk(Authentication user, @RequestBody BulkRequest input) {
    return tasks.bulk(user.getName(), input.ids(), input.action());
  }
}
