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
 * Railway's Redis add-on exposes a single REDIS_URL env var in the form
 * redis://user:password@host:port (rediss:// when TLS is required). Spring
 * needs these as separate spring.data.redis.* properties, so this parses
 * REDIS_URL (when present) and injects them ahead of application.yml's own
 * defaults. Absent REDIS_URL (local dev), this is a no-op and the
 * REDIS_HOST/REDIS_PORT placeholders in application.yml apply as before.
 */
public class RedisUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String redisUrl = environment.getProperty("REDIS_URL");
        if (redisUrl == null || redisUrl.isBlank()) {
            return;
        }

        URI uri = URI.create(redisUrl);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("spring.data.redis.host", uri.getHost());
        properties.put("spring.data.redis.port", uri.getPort() != -1 ? uri.getPort() : 6379);

        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            String[] parts = userInfo.split(":", 2);
            // Redis AUTH without ACLs uses a password only, so the URL often
            // carries a throwaway user ("default") that must not be sent.
            String password = parts.length > 1 ? parts[1] : parts[0];
            properties.put("spring.data.redis.password", decode(password));
            if (parts.length > 1 && !parts[0].isBlank() && !"default".equals(parts[0])) {
                properties.put("spring.data.redis.username", decode(parts[0]));
            }
        }

        if ("rediss".equals(uri.getScheme())) {
            properties.put("spring.data.redis.ssl.enabled", true);
        }

        environment.getPropertySources()
                .addFirst(new MapPropertySource("redisUrlProperties", properties));
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
