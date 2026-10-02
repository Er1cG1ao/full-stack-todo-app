package com.example.todobackend.exception;

/**
 * Business exception: the submitted todo is invalid (e.g. the description is too short). Ends up as
 * a 400.
 */
public class InvalidTodoException extends RuntimeException {

  public InvalidTodoException(String message) {
    super(message);
  }
}
