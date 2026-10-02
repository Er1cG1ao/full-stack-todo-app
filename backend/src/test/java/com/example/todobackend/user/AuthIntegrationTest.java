package com.example.todobackend.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
      "spring.datasource.url=jdbc:h2:mem:auth-test;DB_CLOSE_DELAY=-1",
      "spring.jpa.hibernate.ddl-auto=validate",
      "app.demo-data.enabled=false"
    })
class AuthIntegrationTest {
  @LocalServerPort int port;
  @Autowired AppUserRepository users;
  @Autowired PasswordEncoder passwords;
  private final ObjectMapper json = new ObjectMapper();
  private static final String PASSWORD = "correct-horse-42";
  private String timeZone = java.time.ZoneId.systemDefault().getId();

  private HttpClient browser() {
    return HttpClient.newBuilder()
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
        .build();
  }

  private HttpResponse<String> request(
      HttpClient client, String method, String path, String body, String contentType, boolean csrf)
      throws Exception {
    var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    builder.header("X-Time-Zone", timeZone);
    if (csrf) {
      var token = json.readTree(request(client, "GET", "/auth/csrf", "", "", false).body());
      builder.header(token.get("headerName").asString(), token.get("token").asString());
    }
    if (!contentType.isEmpty()) builder.header("Content-Type", contentType);
    builder.method(
        method,
        body.isEmpty()
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(body));
    return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> register(HttpClient client, String name, String password)
      throws Exception {
    return request(
        client,
        "POST",
        "/auth/register",
        json.writeValueAsString(new AuthController.Credentials(name, password)),
        "application/json",
        true);
  }

  private HttpResponse<String> login(HttpClient client, String name, String password)
      throws Exception {
    return request(
        client,
        "POST",
        "/auth/login",
        "username=" + name + "&password=" + password,
        "application/x-www-form-urlencoded",
        true);
  }

  private String username() {
    return "user_" + UUID.randomUUID().toString().replace("-", "");
  }

  @Test
  void registrationStoresOnlyHashesAndRejectsDuplicatesAndInvalidInput() throws Exception {
    var client = browser();
    String name = username();
    assertThat(register(client, name, PASSWORD).statusCode()).isEqualTo(201);
    var hash = users.findByUsername(name).orElseThrow().getPasswordHash();
    assertThat(hash).startsWith("$2a$12$").isNotEqualTo(PASSWORD);
    assertThat(passwords.matches(PASSWORD, hash)).isTrue();
    assertThat(register(client, name, PASSWORD).statusCode()).isEqualTo(409);
    assertThat(register(client, "bad name", PASSWORD).statusCode()).isEqualTo(400);
    assertThat(register(client, username(), "short").statusCode()).isEqualTo(400);
    assertThat(register(client, username(), "界".repeat(25)).statusCode()).isEqualTo(400);
  }

  @Test
  void loginRotatesSessionAndLogoutInvalidatesIt() throws Exception {
    var client = browser();
    String name = username();
    register(client, name, PASSWORD);
    var manager = (CookieManager) client.cookieHandler().orElseThrow();
    String before =
        manager.getCookieStore().getCookies().stream()
            .filter(cookie -> cookie.getName().equals("JSESSIONID"))
            .findFirst()
            .orElseThrow()
            .getValue();
    assertThat(login(client, name, "incorrect").statusCode()).isEqualTo(401);
    assertThat(request(client, "GET", "/auth/me", "", "", false).statusCode()).isEqualTo(401);
    var success = login(client, name, PASSWORD);
    assertThat(success.statusCode()).isEqualTo(204);
    assertThat(success.headers().allValues("set-cookie").toString())
        .contains("HttpOnly", "SameSite=Lax");
    String after =
        manager.getCookieStore().getCookies().stream()
            .filter(cookie -> cookie.getName().equals("JSESSIONID"))
            .findFirst()
            .orElseThrow()
            .getValue();
    assertThat(after).isNotEqualTo(before);
    assertThat(request(client, "GET", "/auth/me", "", "", false).body())
        .contains(name)
        .doesNotContain("password");
    assertThat(request(client, "POST", "/auth/logout", "", "", true).statusCode()).isEqualTo(204);
    assertThat(request(client, "GET", "/auth/me", "", "", false).statusCode()).isEqualTo(401);
  }

  @Test
  void blocksAnonymousAndCrossUserTodoAccessForEveryOperation() throws Exception {
    var owner = browser();
    var other = browser();
    String ownerName = username(), otherName = username();
    register(owner, ownerName, PASSWORD);
    register(other, otherName, PASSWORD);
    login(owner, ownerName, PASSWORD);
    login(other, otherName, PASSWORD);
    String path = "/users/" + ownerName + "/todos";
    String todo = "{\"description\":\"Private task\",\"targetDate\":\"2026-12-01\",\"done\":false}";
    assertThat(request(browser(), "GET", path, "", "", false).statusCode()).isEqualTo(401);
    var created = request(owner, "POST", path, todo, "application/json", true);
    assertThat(created.statusCode()).isEqualTo(201);
    String item = path + "/" + json.readTree(created.body()).get("id").asLong();
    assertThat(request(other, "GET", path, "", "", false).statusCode()).isEqualTo(403);
    assertThat(request(other, "GET", item, "", "", false).statusCode()).isEqualTo(403);
    assertThat(request(other, "POST", path, todo, "application/json", true).statusCode())
        .isEqualTo(403);
    assertThat(request(other, "PUT", item, todo, "application/json", true).statusCode())
        .isEqualTo(403);
    assertThat(request(other, "DELETE", item, "", "", true).statusCode()).isEqualTo(403);
    String otherPath =
        "/users/" + otherName + "/todos/" + json.readTree(created.body()).get("id").asLong();
    assertThat(request(other, "GET", otherPath, "", "", false).statusCode()).isEqualTo(404);
    assertThat(request(owner, "PUT", item, todo, "application/json", true).statusCode())
        .isEqualTo(200);
    assertThat(request(owner, "DELETE", item, "", "", true).statusCode()).isEqualTo(204);
  }

  @Test
  void csrfIsRequiredForRegistrationLoginLogoutAndTodoWrites() throws Exception {
    var client = browser();
    String name = username();
    assertThat(
            request(client, "POST", "/auth/register", "{}", "application/json", false).statusCode())
        .isEqualTo(403);
    register(client, name, PASSWORD);
    assertThat(
            request(
                    client,
                    "POST",
                    "/auth/login",
                    "username=" + name + "&password=" + PASSWORD,
                    "application/x-www-form-urlencoded",
                    false)
                .statusCode())
        .isEqualTo(403);
    login(client, name, PASSWORD);
    assertThat(request(client, "POST", "/auth/logout", "", "", false).statusCode()).isEqualTo(403);
    assertThat(
            request(client, "POST", "/users/" + name + "/todos", "{}", "application/json", false)
                .statusCode())
        .isEqualTo(403);
  }

  @Test
  void legacyAccountsWithoutCredentialsCannotBeClaimedOrLoggedIn() throws Exception {
    String name = username();
    users.saveAndFlush(new AppUser(name));
    var client = browser();
    assertThat(register(client, name, PASSWORD).statusCode()).isEqualTo(409);
    assertThat(login(client, name, "dummy").statusCode()).isEqualTo(401);
  }

  @Test
  void allowsCredentialedCorsOnlyForConfiguredFrontend() throws Exception {
    var allowed =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/auth/login"))
            .header("Origin", "http://localhost:4200")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "X-CSRF-TOKEN")
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
            .build();
    var response = browser().send(allowed, HttpResponse.BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("access-control-allow-credentials")).contains("true");
    assertThat(response.headers().firstValue("access-control-allow-origin"))
        .contains("http://localhost:4200");
    var denied =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/auth/csrf"))
            .header("Origin", "https://untrusted.example")
            .GET()
            .build();
    assertThat(browser().send(denied, HttpResponse.BodyHandlers.ofString()).statusCode())
        .isEqualTo(403);
  }

  private HttpClient account() throws Exception {
    var client = browser();
    var name = username();
    assertThat(register(client, name, PASSWORD).statusCode()).isEqualTo(201);
    assertThat(login(client, name, PASSWORD).statusCode()).isEqualTo(204);
    return client;
  }

  private HttpResponse<String> api(HttpClient client, String method, String path, Object body)
      throws Exception {
    return request(
        client,
        method,
        path,
        body == null ? "" : json.writeValueAsString(body),
        body == null ? "" : "application/json",
        !method.equals("GET"));
  }

  private com.example.todobackend.todo.Todo task(String title) {
    return new com.example.todobackend.todo.Todo(0L, "spoofed", title, null, false);
  }

  private com.example.todobackend.todo.Todo create(
      HttpClient client, com.example.todobackend.todo.Todo task) throws Exception {
    var response = api(client, "POST", "/api/tasks", task);
    assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
    return json.readValue(response.body(), com.example.todobackend.todo.Todo.class);
  }

  @Test
  void richTaskLifecycleRequiresVersionsAndPreservesMetadata() throws Exception {
    var client = account();
    var invalid = task("Invalid project");
    invalid.setProject("Nonexistent project");
    assertThat(api(client, "POST", "/api/tasks", invalid).statusCode()).isEqualTo(400);
    var input = task("Plan the launch");
    input.setNotes("Private notes <script>alert(1)</script>");
    input.setPriority("HIGH");
    input.setTags("work, launch");
    input.setStarred(true);
    input.setSubtasks(
        java.util.List.of(
            new com.example.todobackend.todo.Todo.Subtask("step-1", "Review checklist", false)));
    var created = create(client, input);
    assertThat(created.getUsername()).isNotEqualTo("spoofed");
    assertThat(created.getTargetDate()).isNull();
    assertThat(created.getCreatedAt()).isNotNull();
    created.setDone(true);
    var result = api(client, "PUT", "/api/tasks/" + created.getId(), created);
    assertThat(result.statusCode()).isEqualTo(200);
    var updated = json.readValue(result.body(), com.example.todobackend.todo.Todo.class);
    assertThat(updated.getCompletedAt()).isNotNull();
    assertThat(updated.getNotes()).isEqualTo(input.getNotes());
    assertThat(updated.getSubtasks()).hasSize(1);
    assertThat(updated.getVersion()).isGreaterThan(created.getVersion());
    assertThat(api(client, "PUT", "/api/tasks/" + created.getId(), created).statusCode())
        .isEqualTo(409);
    updated.setVersion(null);
    assertThat(api(client, "PUT", "/api/tasks/" + created.getId(), updated).statusCode())
        .isEqualTo(400);
    assertThat(
            api(client, "DELETE", "/api/tasks/" + created.getId() + "/permanent", null)
                .statusCode())
        .isEqualTo(400);
    assertThat(api(client, "DELETE", "/api/tasks/" + created.getId(), null).statusCode())
        .isEqualTo(204);
    assertThat(api(client, "GET", "/api/tasks/" + created.getId(), null).statusCode())
        .isEqualTo(404);
    assertThat(api(client, "GET", "/api/tasks?trash=true", null).body())
        .contains("Plan the launch");
    assertThat(api(client, "POST", "/api/tasks/" + created.getId() + "/restore", null).statusCode())
        .isEqualTo(200);
    assertThat(api(client, "GET", "/api/tasks?trash=true", null).body()).isEqualTo("[]");
    api(client, "DELETE", "/api/tasks/" + created.getId(), null);
    assertThat(
            api(client, "DELETE", "/api/tasks/" + created.getId() + "/permanent", null)
                .statusCode())
        .isEqualTo(204);
    assertThat(api(client, "GET", "/api/tasks", null).body()).isEqualTo("[]");
  }

  @Test
  void canonicalApiAndAtomicBulkCannotReadOrModifyOtherAccounts() throws Exception {
    var owner = account();
    var other = account();
    var mine = create(owner, task("Mine"));
    var theirs = create(other, task("Theirs"));
    assertThat(api(browser(), "GET", "/api/tasks", null).statusCode()).isEqualTo(401);
    for (String operation : java.util.List.of("GET", "PUT", "DELETE"))
      assertThat(
              api(
                      other,
                      operation,
                      "/api/tasks/" + mine.getId(),
                      operation.equals("PUT") ? mine : null)
                  .statusCode())
          .isEqualTo(404);
    assertThat(api(other, "POST", "/api/tasks/" + mine.getId() + "/restore", null).statusCode())
        .isEqualTo(404);
    assertThat(api(other, "DELETE", "/api/tasks/" + mine.getId() + "/permanent", null).statusCode())
        .isEqualTo(404);
    var mixed =
        new com.example.todobackend.todo.TaskController.BulkRequest(
            java.util.List.of(mine.getId(), theirs.getId()), "complete");
    assertThat(api(owner, "POST", "/api/tasks/bulk", mixed).statusCode()).isEqualTo(404);
    assertThat(
            json.readTree(api(owner, "GET", "/api/tasks/" + mine.getId(), null).body())
                .get("done")
                .asBoolean())
        .isFalse();
    assertThat(
            api(
                    owner,
                    "POST",
                    "/api/tasks/bulk",
                    java.util.Map.of("ids", java.util.List.of(mine.getId()), "action", "complete"))
                .statusCode())
        .isEqualTo(200);
    assertThat(
            api(
                    owner,
                    "POST",
                    "/api/tasks/bulk",
                    java.util.Map.of(
                        "ids", java.util.List.of(mine.getId(), mine.getId()), "action", "trash"))
                .statusCode())
        .isEqualTo(400);
    assertThat(
            api(
                    owner,
                    "POST",
                    "/api/tasks/bulk",
                    java.util.Map.of(
                        "ids", java.util.Arrays.asList((Long) null), "action", "trash"))
                .statusCode())
        .isEqualTo(400);
    assertThat(
            api(
                    owner,
                    "POST",
                    "/api/tasks/bulk",
                    java.util.Map.of("ids", java.util.List.of(mine.getId())))
                .statusCode())
        .isEqualTo(400);
  }

  @Test
  void recurringTaskCreatesExactlyOneNextOccurrenceEvenAfterReopening() throws Exception {
    timeZone = "Pacific/Kiritimati";
    var client = account();
    var input = task("Weekly review");
    input.setRecurrence("WEEKLY");
    input.setTargetDate(java.time.LocalDate.now().minusDays(10));
    input.setSubtasks(
        java.util.List.of(
            new com.example.todobackend.todo.Todo.Subtask("review", "Read notes", true)));
    var original = create(client, input);
    original.setDone(true);
    var completed =
        json.readValue(
            api(client, "PUT", "/api/tasks/" + original.getId(), original).body(),
            com.example.todobackend.todo.Todo.class);
    var all =
        json.readValue(
            api(client, "GET", "/api/tasks", null).body(),
            com.example.todobackend.todo.Todo[].class);
    assertThat(all).hasSize(2);
    var next = java.util.Arrays.stream(all).filter(t -> !t.isDone()).findFirst().orElseThrow();
    assertThat(next.getTargetDate())
        .isEqualTo(java.time.LocalDate.now(java.time.ZoneId.of(timeZone)).plusWeeks(1));
    assertThat(next.getSubtasks().get(0).done()).isFalse();
    completed.setDone(false);
    var reopened =
        json.readValue(
            api(client, "PUT", "/api/tasks/" + original.getId(), completed).body(),
            com.example.todobackend.todo.Todo.class);
    reopened.setDone(true);
    assertThat(api(client, "PUT", "/api/tasks/" + original.getId(), reopened).statusCode())
        .isEqualTo(200);
    assertThat(json.readTree(api(client, "GET", "/api/tasks", null).body()).size()).isEqualTo(2);
  }

  @Test
  void projectRenameAndRemovalKeepTasksAndRejectOtherOwners() throws Exception {
    var client = account();
    var other = account();
    var project =
        api(client, "POST", "/api/projects", java.util.Map.of("name", "Work", "color", "#708965"));
    assertThat(project.statusCode()).isEqualTo(201);
    var id = json.readTree(project.body()).get("id").asLong();
    var input = task("Project task");
    input.setProject("Work");
    var created = create(client, input);
    assertThat(
            api(
                    client,
                    "POST",
                    "/api/projects",
                    java.util.Map.of("name", "Work", "color", "#708965"))
                .statusCode())
        .isEqualTo(409);
    assertThat(
            api(
                    other,
                    "PUT",
                    "/api/projects/" + id,
                    java.util.Map.of("name", "Stolen", "color", "#708965"))
                .statusCode())
        .isEqualTo(404);
    assertThat(api(other, "DELETE", "/api/projects/" + id, null).statusCode()).isEqualTo(404);
    assertThat(
            api(
                    client,
                    "PUT",
                    "/api/projects/" + id,
                    java.util.Map.of("name", "Career", "color", "#638eae"))
                .statusCode())
        .isEqualTo(200);
    assertThat(
            json.readTree(api(client, "GET", "/api/tasks/" + created.getId(), null).body())
                .get("project")
                .asString())
        .isEqualTo("Career");
    assertThat(api(client, "DELETE", "/api/projects/" + id, null).statusCode()).isEqualTo(204);
    assertThat(
            json.readTree(api(client, "GET", "/api/tasks/" + created.getId(), null).body())
                .get("project")
                .asString())
        .isEmpty();
  }

  @Test
  void backupImportReassignsOwnershipDoesNotOverwriteAndRollsBackInvalidData() throws Exception {
    var source = account();
    var target = account();
    create(source, task("Source task"));
    var deleted = create(source, task("Restorable task"));
    api(source, "DELETE", "/api/tasks/" + deleted.getId(), null);
    var backup =
        json.readValue(
            api(source, "GET", "/api/backup", null).body(),
            com.example.todobackend.todo.BackupController.Backup.class);
    assertThat(json.writeValueAsString(backup))
        .doesNotContain("password", "credentialVersion", "passwordHash");
    create(target, task("Keep existing"));
    assertThat(api(target, "POST", "/api/backup/import", backup).statusCode()).isEqualTo(200);
    assertThat(json.readTree(api(target, "GET", "/api/tasks", null).body()).size()).isEqualTo(2);
    assertThat(json.readTree(api(target, "GET", "/api/tasks?trash=true", null).body()).size())
        .isEqualTo(1);
    var malformed =
        new com.example.todobackend.todo.BackupController.Backup(
            1,
            null,
            "spoofed",
            java.util.List.of(task("Should roll back"), task("")),
            java.util.List.of());
    assertThat(api(target, "POST", "/api/backup/import", malformed).statusCode()).isEqualTo(400);
    assertThat(json.readTree(api(target, "GET", "/api/tasks", null).body()).size()).isEqualTo(2);
  }

  @Test
  void passwordChangeInvalidatesAllSessionsAndRequiresCurrentPassword() throws Exception {
    var first = browser();
    var second = browser();
    var name = username();
    register(first, name, PASSWORD);
    login(first, name, PASSWORD);
    login(second, name, PASSWORD);
    assertThat(
            api(
                    first,
                    "POST",
                    "/auth/password",
                    new AuthController.PasswordChange("wrong", "new-password-42"))
                .statusCode())
        .isEqualTo(400);
    assertThat(
            api(
                    first,
                    "POST",
                    "/auth/password",
                    new AuthController.PasswordChange(PASSWORD, "short"))
                .statusCode())
        .isEqualTo(400);
    assertThat(
            api(
                    first,
                    "POST",
                    "/auth/password",
                    new AuthController.PasswordChange(PASSWORD, "new-password-42"))
                .statusCode())
        .isEqualTo(204);
    assertThat(api(first, "GET", "/auth/me", null).statusCode()).isEqualTo(401);
    assertThat(api(second, "GET", "/auth/me", null).statusCode()).isEqualTo(401);
    assertThat(login(first, name, PASSWORD).statusCode()).isEqualTo(401);
    assertThat(login(first, name, "new-password-42").statusCode()).isEqualTo(204);
  }

  @Test
  void recoveryKeysAreHashedReplacedSingleUseAndInvalidateOldSessions() throws Exception {
    var client = browser();
    var name = username();
    var registered = register(client, name, PASSWORD);
    assertThat(registered.statusCode()).isEqualTo(201);
    String original = json.readTree(registered.body()).get("recoveryCode").asString();
    assertThat(original).matches("DAYLIGHT-[A-Za-z0-9_-]{43}");
    assertThat(users.findByUsername(name).orElseThrow().getRecoveryHash())
        .isEqualTo(RecoveryKeys.hash(original))
        .isNotEqualTo(original);
    login(client, name, PASSWORD);
    assertThat(api(client, "GET", "/auth/me", null).body()).doesNotContain("recovery", "password");
    assertThat(
            api(client, "POST", "/auth/recovery-key", new AuthController.RecoveryRequest("wrong"))
                .statusCode())
        .isEqualTo(400);
    var regenerated =
        api(client, "POST", "/auth/recovery-key", new AuthController.RecoveryRequest(PASSWORD));
    assertThat(regenerated.statusCode()).isEqualTo(200);
    String current = json.readTree(regenerated.body()).get("recoveryCode").asString();
    assertThat(current).isNotEqualTo(original);
    assertThat(
            api(
                    browser(),
                    "POST",
                    "/auth/recover",
                    new AuthController.RecoveryInput(name, original, "recovered-password-42"))
                .statusCode())
        .isEqualTo(400);
    assertThat(
            api(
                    browser(),
                    "POST",
                    "/auth/recover",
                    new AuthController.RecoveryInput(name, current, "recovered-password-42"))
                .statusCode())
        .isEqualTo(204);
    assertThat(api(client, "GET", "/auth/me", null).statusCode()).isEqualTo(401);
    assertThat(
            api(
                    browser(),
                    "POST",
                    "/auth/recover",
                    new AuthController.RecoveryInput(name, current, "recovered-password-43"))
                .statusCode())
        .isEqualTo(400);
    assertThat(users.findByUsername(name).orElseThrow().getRecoveryHash()).isNull();
    assertThat(login(client, name, "recovered-password-42").statusCode()).isEqualTo(204);
  }

  @Test
  void healthChecksDatabaseWithoutRevealingDetails() throws Exception {
    var response = api(browser(), "GET", "/health", null);
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).isEqualTo("{\"status\":\"UP\"}");
  }
}
