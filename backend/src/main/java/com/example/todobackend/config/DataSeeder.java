package com.example.todobackend.config;

import com.example.todobackend.todo.TodoEntity;
import com.example.todobackend.todo.TodoRepository;
import com.example.todobackend.user.AppUser;
import com.example.todobackend.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final AppUserRepository userRepository;
    private final TodoRepository todoRepository;

    public DataSeeder(AppUserRepository userRepository, TodoRepository todoRepository) {
        this.userRepository = userRepository;
        this.todoRepository = todoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("app_user not empty {} rows，skipping", userRepository.count());
            return;
        }

        log.info("Initializing DB");

        AppUser alice = userRepository.save(new AppUser("alice"));
        AppUser bob = userRepository.save(new AppUser("bob"));
        userRepository.save(new AppUser("carol"));   // ★ carol 故意一条 todo 都没有

        seedTodo(alice, "Learn SQL", 5, false);
        seedTodo(alice, "Angular Homework", -1, true);
        seedTodo(alice, "Review relational model", -4, false);
        seedTodo(bob, "Grocery", 2, false);
        seedTodo(bob, "Pay rent", -18, true);
        seedTodo(bob, "Make appointment for physical exam", 22, false);

        log.info("Seed data inserted：{} users / {} todos",
                userRepository.count(), todoRepository.count());
    }

    private void seedTodo(AppUser user, String description, int plusDays, boolean done) {
        todoRepository.save(new TodoEntity(
                user, description, LocalDate.now().plusDays(plusDays), done));
    }
}
