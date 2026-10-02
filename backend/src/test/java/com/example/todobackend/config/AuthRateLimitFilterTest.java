package com.example.todobackend.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitFilterTest {
  static class MutableClock extends Clock {
    Instant now = Instant.parse("2026-10-01T00:00:00Z");

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId zone) {
      return this;
    }

    public Instant instant() {
      return now;
    }
  }

  @Test
  void resetsOnlyAfterTheWindowAndSeparatesKeys() {
    var clock = new MutableClock();
    var limiter = new AuthRateLimitFilter(clock);
    for (int i = 0; i < 10; i++)
      assertThat(limiter.allow("account-a", 10, Duration.ofMinutes(15))).isZero();
    assertThat(limiter.allow("account-a", 10, Duration.ofMinutes(15))).isEqualTo(900);
    assertThat(limiter.allow("account-b", 10, Duration.ofMinutes(15))).isZero();
    clock.now = clock.now.plusSeconds(899);
    assertThat(limiter.allow("account-a", 10, Duration.ofMinutes(15))).isEqualTo(1);
    clock.now = clock.now.plusSeconds(1);
    assertThat(limiter.allow("account-a", 10, Duration.ofMinutes(15))).isZero();
  }

  @Test
  void concurrentAttemptsCannotBypassTheLimit() throws Exception {
    var limiter = new AuthRateLimitFilter();
    var pool = Executors.newFixedThreadPool(8);
    try {
      var jobs = new java.util.ArrayList<Callable<Long>>();
      for (int i = 0; i < 100; i++)
        jobs.add(() -> limiter.allow("same", 10, Duration.ofMinutes(15)));
      int allowed = 0;
      for (var result : pool.invokeAll(jobs)) if (result.get() == 0) allowed++;
      assertThat(allowed).isEqualTo(10);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void returnsRetryAfterAndIgnoresSpoofedForwardedIps() throws Exception {
    var limiter = new AuthRateLimitFilter();
    var request = new MockHttpServletRequest("POST", "/auth/login");
    request.setServletPath("/auth/login");
    request.setRemoteAddr("192.0.2.1");
    request.addParameter("username", "alice");
    for (int i = 0; i < 10; i++)
      limiter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});
    request.addHeader("X-Forwarded-For", "192.0.2.200");
    var response = new MockHttpServletResponse();
    limiter.doFilter(
        request,
        response,
        (req, res) -> {
          throw new AssertionError("Throttled request reached authentication");
        });
    assertThat(response.getStatus()).isEqualTo(429);
    assertThat(response.getHeader("Retry-After")).isNotBlank();
    assertThat(response.getContentAsString()).contains("Too many attempts");
  }

  @Test
  void boundedKeyStoreFailsClosedInsteadOfEvictingActiveLimits() {
    var clock = new MutableClock();
    var limiter = new AuthRateLimitFilter(clock);
    for (int i = 0; i < 10000; i++)
      assertThat(limiter.allow("key-" + i, 1, Duration.ofHours(1))).isZero();
    assertThat(limiter.allow("new-key", 1, Duration.ofHours(1))).isPositive();
    assertThat(limiter.allow("key-0", 1, Duration.ofHours(1))).isPositive();
    clock.now = clock.now.plusSeconds(3601);
    assertThat(limiter.allow("new-key", 1, Duration.ofHours(1))).isZero();
  }
}
