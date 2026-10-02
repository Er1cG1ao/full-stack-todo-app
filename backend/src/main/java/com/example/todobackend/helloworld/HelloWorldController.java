package com.example.todobackend.helloworld;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The two endpoints the frontend's Welcome page uses to check that the frontend-to-backend
 * connection actually works.
 *
 * <p>They map to the frontend's service/data/welcome-data.service.ts:
 * executeHelloWorldBeanService() -> GET /hello-world-bean
 * executeHelloWorldServiceWithPathVariable(name) -> GET /hello-world/path-variable/{name}
 */
@RestController
public class HelloWorldController {

  // Step 1-①: expose the JSON greeting used by the frontend connectivity check.
  @GetMapping("/hello-world-bean")
  public HelloWorldBean helloWorldBean() {
    return new HelloWorldBean("Hello World");
  }

  // Step 1-②: capture a path variable and include it in the JSON response.
  @GetMapping("/hello-world/path-variable/{name}")
  public HelloWorldBean helloWorldPathVariable(@PathVariable String name) {
    return new HelloWorldBean("Hello World, %s! (from Spring Boot)".formatted(name));
  }
}
