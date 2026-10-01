package com.example.todobackend.todo;

import java.util.List;

/**
 * The business-layer contract. The controller depends only on this interface,
 * never on a concrete implementation.
 *
 * Why bother with an interface? In lesson7 the in-memory implementation gets swapped for a
 * database one — and TodoResource won't change by a single character. All that moves is which
 * class carries the @Service annotation.
 * This is where the IoC idea from lesson5 actually pays off.
 *
 * Note that the method names express business intent (create / update) rather than the lesson5
 * style of cramming two meanings into one save() (id <= 0 means insert, otherwise update).
 */
public interface TodoService {

    /** Returns only the todos belonging to this user */
    List<Todo> findByUsername(String username);

    /** Throws TodoNotFoundException when nothing matches — never returns null */
    Todo findById(String username, long id);

    /** Creates a todo; the backend assigns the id */
    Todo create(String username, Todo todo);

    /** Updates a todo; throws TodoNotFoundException when nothing matches */
    Todo update(String username, long id, Todo todo);

    /** Deletes a todo; throws TodoNotFoundException when nothing matches */
    void deleteById(String username, long id);

}
