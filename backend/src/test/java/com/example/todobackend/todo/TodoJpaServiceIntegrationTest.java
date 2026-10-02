package com.example.todobackend.todo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.todobackend.exception.InvalidTodoException;
import com.example.todobackend.exception.TodoNotFoundException;
import com.example.todobackend.exception.UserNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:todo-test;DB_CLOSE_DELAY=-1",
      "spring.jpa.hibernate.ddl-auto=validate"
    })
@Transactional
class TodoJpaServiceIntegrationTest {

  @Autowired private TodoService service;
  @Autowired private com.example.todobackend.user.AppUserRepository users;

  @BeforeEach
  void fixtures() {
    users.saveAndFlush(new com.example.todobackend.user.AppUser("alice"));
    service.create("alice", new Todo(0L, null, "Seeded only in tests", null, false));
  }

  @Test
  void completesTheCrudLifecycleForOneUser() {
    List<Todo> initial = service.findByUsername("alice");
    assertThat(initial).isNotEmpty();

    Todo created =
        service.create(
            "alice",
            new Todo(
                -1L, "ignored", "Write integration tests", LocalDate.now().plusDays(7), false));
    assertThat(created.getId()).isPositive();
    assertThat(created.getUsername()).isEqualTo("alice");

    created.setDescription("Finish integration tests");
    created.setDone(true);
    Todo updated = service.update("alice", created.getId(), created);
    assertThat(updated.getDescription()).isEqualTo("Finish integration tests");
    assertThat(updated.isDone()).isTrue();

    Todo fetched = service.findById("alice", created.getId());
    assertThat(fetched.getDescription()).isEqualTo("Finish integration tests");

    service.deleteById("alice", created.getId());
    assertThatThrownBy(() -> service.findById("alice", created.getId()))
        .isInstanceOf(TodoNotFoundException.class);
  }

  @Test
  void rejectsInvalidInputAndUnknownUsers() {
    Todo invalid = new Todo(-1L, null, "", LocalDate.now(), false);

    assertThatThrownBy(() -> service.create("alice", invalid))
        .isInstanceOf(InvalidTodoException.class)
        .hasMessageContaining("1–255 characters");
    assertThatThrownBy(() -> service.findByUsername("nobody"))
        .isInstanceOf(UserNotFoundException.class);
  }
}
