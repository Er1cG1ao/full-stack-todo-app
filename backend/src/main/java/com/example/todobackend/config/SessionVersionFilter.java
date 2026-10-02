package com.example.todobackend.config;

import com.example.todobackend.user.AppUserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionVersionFilter extends OncePerRequestFilter {
  private final AppUserRepository users;

  public SessionVersionFilter(AppUserRepository users) {
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    var session = request.getSession(false);
    if (auth != null && auth.isAuthenticated() && session != null) {
      var user = users.findByUsername(auth.getName()).orElse(null);
      Object version = session.getAttribute("credentialVersion");
      if (user == null || !Long.valueOf(user.getCredentialVersion()).equals(version)) {
        session.invalidate();
        SecurityContextHolder.clearContext();
        response.setStatus(401);
        response.setContentType("application/json");
        response
            .getWriter()
            .write("{\"status\":401,\"message\":\"Your session expired. Please sign in again.\"}");
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
