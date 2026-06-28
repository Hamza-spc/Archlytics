package com.archlytics.ingest;

import com.archlytics.config.ArchlyticsConfig;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FileScanner {

  private static final Set<String> DEFAULT_IGNORED_DIRS =
      Set.of(
          ".git",
          ".idea",
          ".gradle",
          ".mvn",
          "node_modules",
          "target",
          "build",
          "out",
          "dist",
          "bin",
          "generated");

  private FileScanner() {}

  public static List<ScannedFile> scan(Path repoRoot) {
    return scan(repoRoot, ArchlyticsConfig.defaults());
  }

  public static List<ScannedFile> scan(Path repoRoot, ArchlyticsConfig config) {
    Set<String> ignoredDirs = ignoredDirectories(config);
    Set<String> ignoredModules = new HashSet<>(config.ignore.modules);
    List<PathMatcher> pathMatchers = pathMatchers(config.ignore.pathPatterns);
    List<ScannedFile> results = new ArrayList<>();

    try {
      Files.walkFileTree(
          repoRoot,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
              if (ignoredDirs.contains(dir.getFileName().toString())) {
                return FileVisitResult.SKIP_SUBTREE;
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
              if (!file.toString().endsWith(".java")) {
                return FileVisitResult.CONTINUE;
              }

              Path relative = repoRoot.relativize(file);
              if (matchesAnyPattern(relative, pathMatchers)) {
                return FileVisitResult.CONTINUE;
              }

              String module = inferModule(relative);
              if (ignoredModules.contains(module)) {
                return FileVisitResult.CONTINUE;
              }

              results.add(new ScannedFile(file, relative, module));
              return FileVisitResult.CONTINUE;
            }
          });
    } catch (IOException e) {
      throw new IllegalStateException("Failed to scan repository: " + repoRoot, e);
    }

    results.sort((a, b) -> a.relativePath().compareTo(b.relativePath()));
    return results;
  }

  public static Set<String> ignoredDirectories(ArchlyticsConfig config) {
    Set<String> ignored = new HashSet<>(DEFAULT_IGNORED_DIRS);
    ignored.addAll(config.ignore.directories);
    return ignored;
  }

  public static List<PathMatcher> pathMatchers(List<String> patterns) {
    FileSystem fileSystem = FileSystems.getDefault();
    return patterns.stream().map(pattern -> fileSystem.getPathMatcher("glob:" + pattern)).toList();
  }

  public static boolean matchesAnyPattern(Path relativePath, List<PathMatcher> matchers) {
    for (PathMatcher matcher : matchers) {
      if (matcher.matches(relativePath)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Groups files into Maven-style modules: the directory segment immediately before {@code /src/}.
   * Example: {@code java/maroctax-api/src/main/java/...} → {@code maroctax-api}.
   */
  public static String inferModule(Path relativePath) {
    String normalized = relativePath.toString().replace('\\', '/');
    int srcIndex = normalized.indexOf("/src/");
    if (srcIndex > 0) {
      String beforeSrc = normalized.substring(0, srcIndex);
      int lastSlash = beforeSrc.lastIndexOf('/');
      return lastSlash >= 0 ? beforeSrc.substring(lastSlash + 1) : beforeSrc;
    }

    int firstSlash = normalized.indexOf('/');
    if (firstSlash > 0) {
      return normalized.substring(0, firstSlash);
    }

    return ".";
  }
}
