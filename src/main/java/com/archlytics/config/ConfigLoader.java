package com.archlytics.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

public final class ConfigLoader {

  private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

  private ConfigLoader() {}

  public static ArchlyticsConfig load(Path repoRoot) {
    return load(repoRoot, null);
  }

  public static ArchlyticsConfig load(Path repoRoot, Path explicitConfig) {
    Optional<Path> configFile = resolveConfigFile(repoRoot, explicitConfig);
    if (configFile.isEmpty()) {
      return ArchlyticsConfig.defaults();
    }

    try {
      ArchlyticsConfig loaded = YAML.readValue(configFile.get().toFile(), ArchlyticsConfig.class);
      return mergeWithDefaults(loaded);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read config: " + configFile.get(), e);
    }
  }

  public static Optional<Path> resolveConfigFile(Path repoRoot, Path explicitConfig) {
    if (explicitConfig != null) {
      Path normalized = explicitConfig.toAbsolutePath().normalize();
      if (!Files.isRegularFile(normalized)) {
        throw new IllegalArgumentException("Config file not found: " + normalized);
      }
      return Optional.of(normalized);
    }

    return Stream.of(repoRoot.resolve("archlytics.yaml"), repoRoot.resolve("archlytics.yml"))
        .map(Path::normalize)
        .filter(Files::isRegularFile)
        .findFirst();
  }

  static ArchlyticsConfig mergeWithDefaults(ArchlyticsConfig loaded) {
    ArchlyticsConfig defaults = ArchlyticsConfig.defaults();

    if (loaded.rules == null) {
      loaded.rules = defaults.rules;
    } else {
      if (loaded.rules.highCoupling == null) {
        loaded.rules.highCoupling = defaults.rules.highCoupling;
      }
      if (loaded.rules.systemDesign == null) {
        loaded.rules.systemDesign = defaults.rules.systemDesign;
      }
    }

    if (loaded.ignore == null) {
      loaded.ignore = defaults.ignore;
    } else {
      if (loaded.ignore.modules == null) {
        loaded.ignore.modules = defaults.ignore.modules;
      }
      if (loaded.ignore.directories == null) {
        loaded.ignore.directories = defaults.ignore.directories;
      }
      if (loaded.ignore.pathPatterns == null) {
        loaded.ignore.pathPatterns = defaults.ignore.pathPatterns;
      }
    }

    if (loaded.ai == null) {
      loaded.ai = defaults.ai;
    }

    return loaded;
  }
}
