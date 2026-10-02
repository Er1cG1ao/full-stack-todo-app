package com.example.todobackend.config;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
  private final JdbcTemplate database;

  public HealthController(JdbcTemplate database) {
    this.database = database;
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> health() {
    try {
      database.queryForObject("SELECT 1", Integer.class);
      return ResponseEntity.ok(Map.of("status", "UP"));
    } catch (org.springframework.dao.DataAccessException ex) {
      return ResponseEntity.status(503).body(Map.of("status", "DOWN"));
    }
  }
}
