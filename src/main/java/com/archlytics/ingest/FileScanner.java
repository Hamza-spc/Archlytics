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
   * Groups files into Maven-style modules: the directory segment immediately before {@code /src/}.
   * Example: {@code java/maroctax-api/src/main/java/...} → {@code maroctax-api}.
   */
  static String inferModule(Path relativePath) {
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
