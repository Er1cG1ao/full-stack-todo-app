package com.example.todobackend.todo;

import com.example.todobackend.exception.InvalidTodoException;
import com.example.todobackend.exception.TodoNotFoundException;
import com.example.todobackend.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:todo-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class TodoJpaServiceIntegrationTest {

    @Autowired
    private TodoService service;

    @Test
    void completesTheCrudLifecycleForOneUser() {
        List<Todo> initial = service.findByUsername("alice");
        assertThat(initial).isNotEmpty();

        Todo created = service.create("alice", new Todo(
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
        Todo invalid = new Todo(-1L, null, "bad", LocalDate.now(), false);

        assertThatThrownBy(() -> service.create("alice", invalid))
                .isInstanceOf(InvalidTodoException.class)
                .hasMessageContaining("at least 5 characters");
        assertThatThrownBy(() -> service.findByUsername("nobody"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
