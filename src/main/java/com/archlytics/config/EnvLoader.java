package com.archlytics.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class EnvLoader {

  private EnvLoader() {}

  public static Map<String, String> load() {
    Map<String, String> env = new HashMap<>();
    findEnvFile(Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize())
        .ifPresent(path -> loadFile(path, env));
    return env;
  }

  public static Optional<String> get(String key) {
    String fromSystem = System.getenv(key);
    if (fromSystem != null && !fromSystem.isBlank()) {
      return Optional.of(fromSystem.trim());
    }
    return Optional.ofNullable(load().get(key)).filter(v -> !v.isBlank());
  }

  static Optional<Path> findEnvFile() {
    return findEnvFile(Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize());
  }

  static Optional<Path> findEnvFile(Path startDir) {
    Path dir = startDir;
    while (dir != null) {
      Path candidate = dir.resolve(".env");
      if (Files.isRegularFile(candidate)) {
        return Optional.of(candidate);
      }
      dir = dir.getParent();
    }
    return Optional.empty();
  }

  static void loadFile(Path envFile, Map<String, String> env) {
    try {
      for (String line : Files.readAllLines(envFile)) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        int equals = trimmed.indexOf('=');
        if (equals <= 0) {
          continue;
        }
        String key = trimmed.substring(0, equals).trim();
        String value = trimmed.substring(equals + 1).trim();
        if ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'"))) {
          value = value.substring(1, value.length() - 1);
        }
        env.putIfAbsent(key, value);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + envFile, e);
    }
  }
}
