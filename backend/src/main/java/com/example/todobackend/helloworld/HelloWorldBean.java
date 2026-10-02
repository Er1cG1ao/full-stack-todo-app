package com.example.todobackend.helloworld;

/**
 * The shape declared in the frontend's welcome-data.service.ts: { "message": "..." }
 *
 * <p>A record is Java 16+ syntactic sugar that generates the constructor, the accessor (named
 * message()), and equals/hashCode for you. Jackson understands records, so this serializes to
 * {"message":"..."}.
 */
public record HelloWorldBean(String message) {}
