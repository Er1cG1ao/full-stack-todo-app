package com.example.todobackend.todo;

import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * A mirror of the frontend's todo-data.service.ts. The five methods map one to one:
 *
 * <p>retrieveAllTodos -> GET /users/{username}/todos 200 + Todo[] retrieveTodo -> GET
 * /users/{username}/todos/{id} 200 + Todo / 404 createTodo -> POST /users/{username}/todos 201 +
 * Todo + Location updateTodo -> PUT /users/{username}/todos/{id} 200 + Todo / 404 deleteTodo ->
 * DELETE /users/{username}/todos/{id} 204 no content / 404
 *
 * <p>The class-level @RequestMapping already factors out the common prefix, so the methods only
 * spell out what differs: list -> @GetMapping single item -> @GetMapping("/{id}")
 *
 * <p>Note: the annotations you'll need (@GetMapping / @PostMapping / @PathVariable / @RequestBody
 * ...) aren't imported yet — your IDE will offer to add them.
 */
@RestController
@RequestMapping("/users/{username}/todos")
@PreAuthorize("#username == authentication.name")
public class TodoResource {

  // Step 5-②: constructor injection keeps the dependency explicit and testable.
  private final TodoService todoService;

  public TodoResource(TodoService todoService) {
    this.todoService = todoService;
  }

  // Step 5-③: return only the path user's todos.
  @GetMapping
  public List<Todo> getAllTodos(@PathVariable String username) {
    return todoService.findByUsername(username);
  }

  // Step 5-④: retrieve a single resource by owner and id.
  @GetMapping("/{id}")
  public Todo getTodo(@PathVariable String username, @PathVariable long id) {
    return todoService.findById(username, id);
  }

  // Step 6-②: a successful delete returns 204 and no body.
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTodo(@PathVariable String username, @PathVariable long id) {
    todoService.deleteById(username, id);
    return ResponseEntity.noContent().build();
  }

  // Step 7-②: the id and owner in the path take precedence over request-body values.
  @PutMapping("/{id}")
  public Todo updateTodo(
      @PathVariable String username, @PathVariable long id, @RequestBody Todo todo) {
    return todoService.update(username, id, todo);
  }

  // Step 8-②: return 201, the created object and its canonical Location URI.
  @PostMapping
  public ResponseEntity<Todo> createTodo(@PathVariable String username, @RequestBody Todo todo) {
    Todo created = todoService.create(username, todo);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.getId())
            .toUri();
    return ResponseEntity.created(location).body(created);
  }
}
