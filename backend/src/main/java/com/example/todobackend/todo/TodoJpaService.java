package com.example.todobackend.todo;

import com.example.todobackend.exception.TodoNotFoundException;
import com.example.todobackend.exception.InvalidTodoException;
import com.example.todobackend.exception.UserNotFoundException;
import com.example.todobackend.user.AppUser;
import com.example.todobackend.user.AppUserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TodoJpaService implements TodoService {
    private final TodoRepository todoRepository;
    private final AppUserRepository userRepository;

    public TodoJpaService(TodoRepository todoRepository, AppUserRepository userRepository) {
        this.todoRepository = todoRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly=true)
    public List<Todo> findByUsername(String username) {
        AppUser user = requireUser(username);
        List<TodoEntity> found = todoRepository.findByUserId(user.getId(), Sort.by(Sort.Direction.ASC, "id"));
        return found.stream().map(entity -> toApiModel(entity, username)).toList();
    }

    private AppUser requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
    }

    private Todo toApiModel(TodoEntity e, String username) {
        return new Todo(e.getId(), username, e.getDescription(), e.getTargetDate(), e.isDone());
    }

    @Override
    public Todo findById(String username, long id) {
        return toApiModel(requireOwnedTodo(username, id), username);
    }

    @Override
    public Todo create(String username, Todo todo) {
        validate(todo);
        AppUser user = requireUser(username);
        TodoEntity created = todoRepository.save(new TodoEntity(
                user,
                todo.getDescription().trim(),
                todo.getTargetDate(),
                todo.isDone()));
        return toApiModel(created, username);
    }

    @Override
    public Todo update(String username, long id, Todo todo) {
        validate(todo);
        TodoEntity existing = requireOwnedTodo(username, id);

        existing.setDescription(todo.getDescription().trim());
        existing.setTargetDate(todo.getTargetDate());
        existing.setDone(todo.isDone());

        return toApiModel(existing, username);
    }
    private TodoEntity requireOwnedTodo(String username, long id) {
        AppUser user = requireUser(username);
        return todoRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new TodoNotFoundException(username, id));
    }
    @Override
    public void deleteById(String username, long id) {
        todoRepository.delete(requireOwnedTodo(username, id));
    }

    private void validate(Todo todo) {
        if (todo == null) {
            throw new InvalidTodoException("todo body is required");
        }
        if (todo.getDescription() == null || todo.getDescription().trim().length() < 5) {
            throw new InvalidTodoException("description must be at least 5 characters");
        }
        if (todo.getTargetDate() == null) {
            throw new InvalidTodoException("targetDate is required");
        }
    }
}
