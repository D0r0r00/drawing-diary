package com.drawingdiary.backend.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Railway (and Heroku-style) Postgres add-ons expose a single DATABASE_URL
 * env var in the form postgresql://user:password@host:port/dbname. Spring's
 * JDBC driver needs a jdbc:postgresql:// URL plus separate username/password,
 * so this parses DATABASE_URL (when present) and injects the equivalent
 * spring.datasource.* properties ahead of application.yml's own defaults.
 * Absent DATABASE_URL (local dev), this is a no-op and the DB_HOST/DB_PORT/...
 * placeholders in application.yml apply as before.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        URI uri = URI.create(databaseUrl);

        String username = "";
        String password = "";
        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            String[] parts = userInfo.split(":", 2);
            username = decode(parts[0]);
            password = parts.length > 1 ? decode(parts[1]) : "";
        }

        int port = uri.getPort() != -1 ? uri.getPort() : 5432;
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.datasource.url", jdbcUrl);
        properties.put("spring.datasource.username", username);
        properties.put("spring.datasource.password", password);

        environment.getPropertySources()
                .addFirst(new MapPropertySource("databaseUrlProperties", properties));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
