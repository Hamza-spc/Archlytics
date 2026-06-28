package com.archlytics.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SnapshotStoreTest {

  @TempDir Path tempDir;

  @Test
  void saveAndLoad_roundTrip() throws Exception {
    RunSnapshot snapshot =
        new RunSnapshot(
            "2026-06-28 12:00:00",
            tempDir.toString(),
            72,
            "Needs attention",
            10,
            2,
            1,
            List.of("api", "core"),
            java.util.Map.of("api", List.of("core")),
            List.of(new SnapshotViolation("LOW", "high-coupling", "File has many internal dependencies", "App.java")));

    Path saved = SnapshotStore.save(tempDir, snapshot);
    RunSnapshot loaded = SnapshotStore.load(saved);
    RunSnapshot latest = SnapshotStore.loadLatest(tempDir);

    assertTrue(Files.exists(saved));
    assertTrue(Files.exists(SnapshotStore.latestSnapshotPath(tempDir)));
    assertEquals(72, loaded.healthScore());
    assertEquals(72, latest.healthScore());
    assertEquals("App.java", loaded.violations().get(0).evidence());
  }

  @Test
  void resolveCompareTarget_usesLatestKeyword() throws Exception {
    SnapshotStore.save(
        tempDir,
        new RunSnapshot(
            "2026-06-28 12:00:00",
            tempDir.toString(),
            80,
            "Healthy",
            5,
            1,
            0,
            List.of(),
            java.util.Map.of(),
            List.of()));

    Path resolved = SnapshotStore.resolveCompareTarget(tempDir, "latest");
    assertEquals(SnapshotStore.latestSnapshotPath(tempDir), resolved);
  }
}
