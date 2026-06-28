package com.archlytics.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

public final class SnapshotStore {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

  private static final DateTimeFormatter FILE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss");

  private SnapshotStore() {}

  public static Path snapshotDir(Path repoRoot) {
    return repoRoot.resolve(".archlytics/snapshots");
  }

  public static Path latestSnapshotPath(Path repoRoot) {
    return snapshotDir(repoRoot).resolve("latest.json");
  }

  public static Path save(Path repoRoot, RunSnapshot snapshot) throws IOException {
    Path dir = snapshotDir(repoRoot);
    Files.createDirectories(dir);

    Path timestamped = dir.resolve(FILE_TIME.format(LocalDateTime.now()) + ".json");
    write(timestamped, snapshot);
    write(latestSnapshotPath(repoRoot), snapshot);

    return timestamped;
  }

  public static RunSnapshot load(Path snapshotFile) throws IOException {
    if (!Files.isRegularFile(snapshotFile)) {
      throw new IllegalArgumentException("Snapshot not found: " + snapshotFile);
    }
    return MAPPER.readValue(snapshotFile.toFile(), RunSnapshot.class);
  }

  public static RunSnapshot loadLatest(Path repoRoot) throws IOException {
    return load(latestSnapshotPath(repoRoot));
  }

  public static Path resolveCompareTarget(Path repoRoot, String compareTarget) throws IOException {
    if (compareTarget == null || compareTarget.isBlank() || "latest".equalsIgnoreCase(compareTarget)) {
      return latestSnapshotPath(repoRoot);
    }

    Path candidate = Path.of(compareTarget);
    if (Files.isRegularFile(candidate)) {
      return candidate.toAbsolutePath().normalize();
    }

    Path inRepo = repoRoot.resolve(compareTarget).normalize();
    if (Files.isRegularFile(inRepo)) {
      return inRepo;
    }

    throw new IllegalArgumentException("Snapshot not found: " + compareTarget);
  }

  public static RunSnapshot loadMostRecentExcept(Path repoRoot, Path exclude) throws IOException {
    Path dir = snapshotDir(repoRoot);
    if (!Files.isDirectory(dir)) {
      throw new IllegalArgumentException("No snapshots found in " + dir);
    }

    try (Stream<Path> files =
        Files.list(dir)
            .filter(path -> path.toString().endsWith(".json"))
            .filter(path -> !path.getFileName().toString().equals("latest.json"))
            .filter(path -> !path.toAbsolutePath().normalize().equals(exclude.toAbsolutePath().normalize()))
            .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))) {
      Path previous =
          files.findFirst()
              .orElseThrow(() -> new IllegalArgumentException("No previous snapshot found in " + dir));
      return load(previous);
    }
  }

  private static void write(Path path, RunSnapshot snapshot) throws IOException {
    MAPPER.writeValue(path.toFile(), snapshot);
  }
}
