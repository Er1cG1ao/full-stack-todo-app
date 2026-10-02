package com.example.todobackend.todo;

import com.example.todobackend.exception.*;
import com.example.todobackend.user.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class TodoJpaService implements TodoService {
  private final TodoRepository todos;
  private final AppUserRepository users;
  private final ObjectMapper json;

  public TodoJpaService(TodoRepository todos, AppUserRepository users, ObjectMapper json) {
    this.todos = todos;
    this.users = users;
    this.json = json;
  }

  @Transactional(readOnly = true)
  public List<Todo> findByUsername(String username) {
    return list(username, false);
  }

  @Transactional(readOnly = true)
  public List<Todo> list(String username, boolean trash) {
    return todos
        .findByUserId(requireUser(username).getId(), Sort.by(Sort.Direction.DESC, "createdAt"))
        .stream()
        .filter(t -> (t.getDeletedAt() != null) == trash)
        .map(t -> model(t, username))
        .toList();
  }

  public Todo findById(String username, long id) {
    TodoEntity e = owned(username, id);
    if (e.getDeletedAt() != null) throw new TodoNotFoundException(username, id);
    return model(e, username);
  }

  public Todo create(String username, Todo input) {
    validate(input);
    TodoEntity e =
        new TodoEntity(
            requireUser(username),
            input.getDescription().trim(),
            input.getTargetDate(),
            input.isDone());
    apply(e, input);
    return model(todos.saveAndFlush(e), username);
  }

  public Todo update(String username, long id, Todo input) {
    validate(input);
    TodoEntity e = owned(username, id);
    if (e.getDeletedAt() != null) throw new TodoNotFoundException(username, id);
    if (input.getVersion() != null && !input.getVersion().equals(e.getVersion()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This task changed in another window. Refresh and try again.");
    boolean newlyCompleted = !e.isDone() && input.isDone();
    apply(e, input);
    if (newlyCompleted && !"NONE".equals(e.getRecurrence()) && !e.isRecurrenceGenerated()) {
      e.markRecurrenceGenerated();
      Todo next = model(e, username);
      LocalDate base = e.getTargetDate() == null ? LocalDate.now() : e.getTargetDate();
      if (base.isBefore(LocalDate.now())) base = LocalDate.now();
      next.setTargetDate(
          switch (e.getRecurrence()) {
            case "DAILY" -> base.plusDays(1);
            case "WEEKLY" -> base.plusWeeks(1);
            case "MONTHLY" -> base.plusMonths(1);
            default -> base;
          });
      next.setDone(false);
      next.setSubtasks(
          next.getSubtasks().stream()
              .map(s -> new Todo.Subtask(s.id(), s.title(), false))
              .toList());
      create(username, next);
    }
    return model(todos.saveAndFlush(e), username);
  }

  public void deleteById(String username, long id) {
    TodoEntity e = owned(username, id);
    e.setDeletedAt(Instant.now());
  }

  public Todo restore(String username, long id) {
    TodoEntity e = owned(username, id);
    e.setDeletedAt(null);
    return model(todos.saveAndFlush(e), username);
  }

  public void permanentlyDelete(String username, long id) {
    TodoEntity e = owned(username, id);
    if (e.getDeletedAt() == null)
      throw new InvalidTodoException("Move the task to Trash before permanently deleting it");
    todos.delete(e);
  }

  public List<Todo> bulk(String username, List<Long> ids, String action) {
    if (ids == null
        || ids.isEmpty()
        || ids.size() > 100
        || ids.stream().anyMatch(Objects::isNull)
        || new HashSet<>(ids).size() != ids.size())
      throw new InvalidTodoException("Select between 1 and 100 distinct tasks");
    if (action == null || !Set.of("complete", "reopen", "trash", "restore").contains(action))
      throw new InvalidTodoException("Unknown bulk action");
    List<TodoEntity> owned = ids.stream().map(id -> owned(username, id)).toList();
    for (TodoEntity e : owned) {
      if (action.equals("restore")) e.setDeletedAt(null);
      else if (action.equals("trash")) e.setDeletedAt(Instant.now());
      else {
        if (e.getDeletedAt() != null) throw new InvalidTodoException("Restore trashed tasks first");
        Todo input = model(e, username);
        input.setDone(action.equals("complete"));
        update(username, e.getId(), input);
      }
    }
    todos.flush();
    return list(username, false);
  }

  private AppUser requireUser(String username) {
    return users.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));
  }

  private TodoEntity owned(String username, long id) {
    return todos
        .findByIdAndUserId(id, requireUser(username).getId())
        .orElseThrow(() -> new TodoNotFoundException(username, id));
  }

  private Todo model(TodoEntity e, String username) {
    Todo t = new Todo(e.getId(), username, e.getDescription(), e.getTargetDate(), e.isDone());
    t.setNotes(e.getNotes());
    t.setPriority(e.getPriority());
    t.setProject(e.getProject());
    t.setTags(e.getTags());
    t.setRecurrence(e.getRecurrence());
    t.setStarred(e.isStarred());
    t.setSubtasks(Arrays.asList(json.readValue(e.getSubtasksJson(), Todo.Subtask[].class)));
    t.setCreatedAt(e.getCreatedAt());
    t.setUpdatedAt(e.getUpdatedAt());
    t.setCompletedAt(e.getCompletedAt());
    t.setDeletedAt(e.getDeletedAt());
    t.setVersion(e.getVersion());
    return t;
  }

  private void apply(TodoEntity e, Todo t) {
    e.setDescription(t.getDescription().trim());
    e.setTargetDate(t.getTargetDate());
    if (t.isDone() && e.getCompletedAt() == null) e.setCompletedAt(Instant.now());
    if (!t.isDone()) e.setCompletedAt(null);
    e.setDone(t.isDone());
    e.setNotes(t.getNotes() == null ? "" : t.getNotes().trim());
    e.setPriority(t.getPriority());
    e.setProject(t.getProject() == null ? "" : t.getProject().trim());
    e.setTags(
        t.getTags() == null
            ? ""
            : String.join(
                ", ",
                Arrays.stream(t.getTags().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .toList()));
    e.setRecurrence(t.getRecurrence());
    e.setStarred(t.isStarred());
    e.setSubtasksJson(json.writeValueAsString(t.getSubtasks()));
  }

  private void validate(Todo t) {
    if (t == null) throw new InvalidTodoException("Task body is required");
    if (t.getDescription() == null
        || t.getDescription().trim().isEmpty()
        || t.getDescription().trim().length() > 255)
      throw new InvalidTodoException("Task title must contain 1–255 characters");
    if (t.getNotes() != null && t.getNotes().length() > 4000)
      throw new InvalidTodoException("Notes must be 4000 characters or fewer");
    if (t.getProject() != null && t.getProject().length() > 80)
      throw new InvalidTodoException("Project name is too long");
    if (t.getTags() != null && t.getTags().length() > 500)
      throw new InvalidTodoException("Tags are too long");
    if (t.getPriority() == null || !Set.of("LOW", "MEDIUM", "HIGH").contains(t.getPriority()))
      throw new InvalidTodoException("Priority must be LOW, MEDIUM or HIGH");
    if (t.getRecurrence() == null
        || !Set.of("NONE", "DAILY", "WEEKLY", "MONTHLY").contains(t.getRecurrence()))
      throw new InvalidTodoException("Unknown repeat schedule");
    if (t.getSubtasks() == null || t.getSubtasks().size() > 50)
      throw new InvalidTodoException("A task can have up to 50 subtasks");
    Set<String> ids = new HashSet<>();
    for (Todo.Subtask s : t.getSubtasks()) {
      if (s == null
          || s.id() == null
          || s.id().isBlank()
          || s.id().length() > 80
          || !ids.add(s.id())
          || s.title() == null
          || s.title().isBlank()
          || s.title().length() > 200)
        throw new InvalidTodoException(
            "Each subtask needs a unique ID and a title of 1–200 characters");
    }
    if (json.writeValueAsString(t.getSubtasks()).length() > 16000)
      throw new InvalidTodoException("Subtasks are too long in total");
  }
}
