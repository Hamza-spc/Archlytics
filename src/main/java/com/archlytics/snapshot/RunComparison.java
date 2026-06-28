package com.archlytics.snapshot;

import java.util.List;

public record RunComparison(
    RunSnapshot baseline,
    RunSnapshot current,
    int scoreDelta,
    List<SnapshotViolation> newViolations,
    List<SnapshotViolation> resolvedViolations) {

  public String scoreSummary() {
    int previous = baseline.healthScore();
    int now = current.healthScore();
    String direction = scoreDelta > 0 ? "+" : "";
    return previous + " → " + now + " (" + direction + scoreDelta + ")";
  }
}
