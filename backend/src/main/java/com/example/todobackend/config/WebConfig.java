package com.example.todobackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS configuration.
 *
 * <p>In lesson5 every controller got its own @CrossOrigin line — someone always forgets one.
 * Configure it once, centrally, and it applies everywhere.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  /** Read from application.properties; the value after the colon is the fallback default */
  @Value("${app.cors.allowed-origin:http://localhost:4200}")
  private String allowedOrigin;

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    // Step 2-①: configure CORS once for every endpoint used by the Angular app.
    registry
        .addMapping("/**")
        .allowedOrigins(allowedOrigin)
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true)
        .maxAge(3600);
  }
}
