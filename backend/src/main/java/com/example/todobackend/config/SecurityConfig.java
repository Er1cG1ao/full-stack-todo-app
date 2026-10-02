package com.example.todobackend.config;

import com.example.todobackend.user.AppUserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService userDetailsService(AppUserRepository users) {
    return username -> {
      var user =
          users
              .findByUsername(username)
              .filter(account -> account.getPasswordHash() != null)
              .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
      return User.withUsername(user.getUsername())
          .password(user.getPasswordHash())
          .roles("USER")
          .build();
    };
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, AppUserRepository users)
      throws Exception {
    var contexts = new HttpSessionSecurityContextRepository();
    http.cors(cors -> {})
        .securityContext(context -> context.securityContextRepository(contexts))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/auth/csrf",
                        "/auth/register",
                        "/auth/login",
                        "/auth/recover",
                        "/error",
                        "/health",
                        "/hello-world/**",
                        "/hello-world-bean",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/openapi.yaml")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .requestCache(cache -> cache.disable())
        .formLogin(
            login ->
                login
                    .loginProcessingUrl("/auth/login")
                    .securityContextRepository(contexts)
                    .successHandler(
                        (request, response, auth) -> {
                          request
                              .getSession()
                              .setAttribute(
                                  "credentialVersion",
                                  users
                                      .findByUsername(auth.getName())
                                      .orElseThrow()
                                      .getCredentialVersion());
                          response.setStatus(204);
                        })
                    .failureHandler(
                        (request, response, ex) ->
                            error(response, 401, "Invalid username or password")))
        .logout(
            logout ->
                logout
                    .logoutUrl("/auth/logout")
                    .deleteCookies("JSESSIONID")
                    .logoutSuccessHandler((request, response, auth) -> response.setStatus(204)))
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, ex) -> error(response, 401, "Please sign in"))
                    .accessDeniedHandler(
                        (request, response, ex) ->
                            error(response, 403, "Access denied or invalid CSRF token")));
    // CSRF remains enabled, including registration, login and logout.
    http.addFilterBefore(new AuthRateLimitFilter(), UsernamePasswordAuthenticationFilter.class);
    http.addFilterAfter(new SessionVersionFilter(users), SecurityContextHolderFilter.class);
    return http.build();
  }

  private static void error(HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.getWriter().write("{\"status\":" + status + ",\"message\":\"" + message + "\"}");
  }
}
