package com.archlytics.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SnapshotComparerTest {

  @Test
  void compare_detectsScoreDeltaAndViolationChanges() {
    RunSnapshot baseline =
        new RunSnapshot(
            "2026-06-01 10:00:00",
            "/repo",
            70,
            "Needs attention",
            20,
            3,
            2,
            List.of("api", "core"),
            java.util.Map.of("api", List.of("core")),
            List.of(
                new SnapshotViolation("HIGH", "system-design", "Shared kernel bottleneck", "core hub"),
                new SnapshotViolation("LOW", "high-coupling", "File has many internal dependencies", "Old.java")));

    RunSnapshot current =
        new RunSnapshot(
            "2026-06-02 10:00:00",
            "/repo",
            85,
            "Healthy",
            22,
            3,
            2,
            List.of("api", "core"),
            java.util.Map.of("api", List.of("core")),
            List.of(
                new SnapshotViolation("HIGH", "system-design", "Shared kernel bottleneck", "core hub"),
                new SnapshotViolation("MEDIUM", "system-design", "Layer bypass detected", "api -> core")));

    RunComparison comparison = SnapshotComparer.compare(baseline, current);

    assertEquals(15, comparison.scoreDelta());
    assertEquals("70 → 85 (+15)", comparison.scoreSummary());
    assertEquals(1, comparison.newViolations().size());
    assertTrue(comparison.newViolations().get(0).title().contains("Layer bypass"));
    assertEquals(1, comparison.resolvedViolations().size());
    assertTrue(comparison.resolvedViolations().get(0).title().contains("File has many internal dependencies"));
  }
}
