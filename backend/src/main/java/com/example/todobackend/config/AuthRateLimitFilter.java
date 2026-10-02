package com.example.todobackend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bounded, atomic rate limits for a single application instance. Never trusts forwarded IP headers.
 */
public class AuthRateLimitFilter extends OncePerRequestFilter {
  private record Window(Instant start, int count) {}

  private final Map<String, Window> windows = new LinkedHashMap<>();
  private final Clock clock;

  public AuthRateLimitFilter() {
    this(Clock.systemUTC());
  }

  public AuthRateLimitFilter(Clock clock) {
    this.clock = clock;
  }

  public synchronized long allow(String key, int limit, Duration duration) {
    Instant now = clock.instant();
    windows.entrySet().removeIf(e -> e.getValue().start().plus(Duration.ofHours(1)).isBefore(now));
    Window w = windows.get(key);
    if (w == null || !w.start().plus(duration).isAfter(now)) w = new Window(now, 0);
    if (w.count() >= limit)
      return Math.max(1, Duration.between(now, w.start().plus(duration)).toSeconds());
    // Reject new keys if the budget is full; do not evict active limits to allow bypasses.
    if (!windows.containsKey(key) && windows.size() >= 10000) return 60;
    windows.put(key, new Window(w.start(), w.count() + 1));
    return 0;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String path = request.getServletPath();
    if (request.getMethod().equals("POST")
        && Set.of(
                "/auth/login",
                "/auth/register",
                "/auth/password",
                "/auth/recover",
                "/auth/recovery-key")
            .contains(path)) {
      String ip = request.getRemoteAddr();
      long wait = allow("ip:" + ip, 60, Duration.ofMinutes(5));
      if (wait == 0 && path.equals("/auth/login")) {
        String name =
            Objects.toString(request.getParameter("username"), "")
                .substring(
                    0,
                    Math.min(50, Objects.toString(request.getParameter("username"), "").length()));
        wait = allow("login:" + ip + ":" + name, 10, Duration.ofMinutes(15));
      }
      if (wait == 0 && path.equals("/auth/register"))
        wait = allow("register:" + ip, 20, Duration.ofHours(1));
      if (wait == 0 && path.equals("/auth/recover"))
        wait = allow("recover:" + ip, 10, Duration.ofMinutes(15));
      if (wait > 0) {
        response.setStatus(429);
        response.setHeader("Retry-After", Long.toString(wait));
        response.setContentType("application/json");
        response
            .getWriter()
            .write(
                "{\"status\":429,\"message\":\"Too many attempts. Please try again"
                    + " later.\",\"retryAfter\":"
                    + wait
                    + "}");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
