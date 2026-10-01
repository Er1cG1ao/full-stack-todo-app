package com.example.todobackend.todo;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<TodoEntity, Long> {

    List<TodoEntity> findByUserId(Long userId, Sort sort);

    Optional<TodoEntity> findByIdAndUserId(Long id, Long userId);
}