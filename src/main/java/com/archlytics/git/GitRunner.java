package com.archlytics.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class GitRunner {

  private GitRunner() {}

  public static boolean isGitRepository(Path repoRoot) {
    try {
      run(repoRoot, "rev-parse", "--git-dir");
      return true;
    } catch (IllegalStateException e) {
      return false;
    }
  }

  public static List<String> changedJavaFiles(Path repoRoot, String baseRef, String headRef) {
    return run(repoRoot, "diff", "--name-only", baseRef + "..." + headRef, "--", "*.java").stream()
        .filter(line -> line.endsWith(".java"))
        .toList();
  }

  public static List<String> listJavaFilesAtRef(Path repoRoot, String ref) {
    List<String> lines = run(repoRoot, "ls-tree", "-r", "--name-only", ref, "--");
    List<String> javaFiles = new ArrayList<>();
    for (String line : lines) {
      if (line.endsWith(".java")) {
        javaFiles.add(line);
      }
    }
    return javaFiles;
  }

  public static String showFileAtRef(Path repoRoot, String ref, String relativePath) {
    String gitPath = relativePath.replace('\\', '/');
    return String.join("\n", run(repoRoot, "show", ref + ":" + gitPath));
  }

  static List<String> run(Path repoRoot, String... args) {
    try {
      ProcessBuilder builder = new ProcessBuilder();
      List<String> command = new ArrayList<>();
      command.add("git");
      command.add("-C");
      command.add(repoRoot.toString());
      command.addAll(List.of(args));
      builder.command(command);
      builder.redirectErrorStream(true);

      Process process = builder.start();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      boolean finished = process.waitFor(30, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new IllegalStateException("Git command timed out: " + String.join(" ", command));
      }
      if (process.exitValue() != 0) {
        throw new IllegalStateException(
            "Git command failed (" + process.exitValue() + "): " + String.join(" ", command) + "\n" + output);
      }

      return output.lines().filter(line -> !line.isBlank()).toList();
    } catch (IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Failed to run git in " + repoRoot, e);
    }
  }
}
