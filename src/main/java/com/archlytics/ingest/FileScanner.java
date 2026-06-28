package com.archlytics.ingest;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class FileScanner {

  private static final Set<String> IGNORED_DIRS =
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
    List<ScannedFile> results = new ArrayList<>();

    try {
      Files.walkFileTree(
          repoRoot,
          new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
              if (IGNORED_DIRS.contains(dir.getFileName().toString())) {
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
              String module = inferModule(relative);
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

  /**
   * Groups files into modules using Maven-style layout: everything under {@code src/main/java} or
   * {@code src/test/java} is grouped by the first path segment after {@code java/} (the package
   * root), or by the nearest {@code pom.xml} parent directory when present.
   */
  static String inferModule(Path relativePath) {
    String normalized = relativePath.toString().replace('\\', '/');
    int javaIndex = normalized.indexOf("src/main/java/");
    if (javaIndex >= 0) {
      String afterJava = normalized.substring(javaIndex + "src/main/java/".length());
      int slash = afterJava.indexOf('/');
      if (slash > 0) {
        return afterJava.substring(0, slash);
      }
    }

    int firstSlash = normalized.indexOf('/');
    if (firstSlash > 0) {
      return normalized.substring(0, firstSlash);
    }

    return ".";
  }
}
