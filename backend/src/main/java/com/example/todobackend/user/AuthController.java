package com.example.todobackend.user;

import java.nio.charset.StandardCharsets;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AppUserRepository users;
  private final PasswordEncoder passwords;

  public AuthController(AppUserRepository users, PasswordEncoder passwords) {
    this.users = users;
    this.passwords = passwords;
  }

  public record Credentials(String username, String password) {}

  public record UserResponse(String username) {}

  public record CsrfResponse(String headerName, String token) {}

  public record PasswordChange(String currentPassword, String newPassword) {}

  public record RegistrationResponse(String username, String recoveryCode) {}

  public record RecoveryInput(String username, String recoveryCode, String newPassword) {}

  public record RecoveryRequest(String currentPassword) {}

  public record RecoveryResponse(String recoveryCode) {}

  @PostMapping("/recovery-key")
  public RecoveryResponse generateRecoveryKey(
      Authentication auth, @RequestBody RecoveryRequest input) {
    var user = users.findByUsername(auth.getName()).orElseThrow();
    if (input.currentPassword() == null
        || !passwords.matches(input.currentPassword(), user.getPasswordHash()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
    String key = RecoveryKeys.generate();
    user.setRecoveryHash(RecoveryKeys.hash(key));
    users.saveAndFlush(user);
    return new RecoveryResponse(key);
  }

  @PostMapping("/recover")
  public ResponseEntity<Void> recover(@RequestBody RecoveryInput input) {
    validatePassword(input.newPassword());
    var user =
        input.username() == null ? null : users.findByUsername(input.username()).orElse(null);
    if (user == null || !RecoveryKeys.matches(input.recoveryCode(), user.getRecoveryHash()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username or recovery key");
    user.changePassword(passwords.encode(input.newPassword()));
    user.setRecoveryHash(null);
    users.saveAndFlush(user);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/password")
  public ResponseEntity<Void> changePassword(
      Authentication auth,
      @RequestBody PasswordChange input,
      jakarta.servlet.http.HttpServletRequest request) {
    var user = users.findByUsername(auth.getName()).orElseThrow();
    if (input.currentPassword() == null
        || !passwords.matches(input.currentPassword(), user.getPasswordHash()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
    validatePassword(input.newPassword());
    if (passwords.matches(input.newPassword(), user.getPasswordHash()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a different password");
    user.changePassword(passwords.encode(input.newPassword()));
    users.saveAndFlush(user);
    if (request.getSession(false) != null) request.getSession(false).invalidate();
    org.springframework.security.core.context.SecurityContextHolder.clearContext();
    return ResponseEntity.noContent().build();
  }

  private void validatePassword(String password) {
    if (password == null
        || password.length() < 8
        || password.getBytes(StandardCharsets.UTF_8).length > 72)
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Password must contain at least 8 characters and at most 72 UTF-8 bytes");
  }

  @GetMapping("/csrf")
  public CsrfResponse csrf(CsrfToken token) {
    return new CsrfResponse(token.getHeaderName(), token.getToken());
  }

  @GetMapping("/me")
  public UserResponse currentUser(Authentication authentication) {
    return new UserResponse(authentication.getName());
  }

  @PostMapping("/register")
  public ResponseEntity<RegistrationResponse> register(@RequestBody Credentials credentials) {
    String username = credentials.username();
    String password = credentials.password();
    if (username == null || !username.matches("[a-zA-Z0-9_.-]{3,50}")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Username must contain 3–50 letters, numbers, dots, hyphens or underscores");
    }
    validatePassword(password);
    if (users.findByUsername(username).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
    }
    String key = RecoveryKeys.generate();
    try {
      var user = new AppUser(username, passwords.encode(password));
      user.setRecoveryHash(RecoveryKeys.hash(key));
      users.saveAndFlush(user);
    } catch (DataIntegrityViolationException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(new RegistrationResponse(username, key));
  }
}
