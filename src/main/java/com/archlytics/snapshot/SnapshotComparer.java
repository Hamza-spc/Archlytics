package com.archlytics.snapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SnapshotComparer {

  private SnapshotComparer() {}

  public static RunComparison compare(RunSnapshot baseline, RunSnapshot current) {
    Set<String> baselineKeys = fingerprints(baseline.violations());
    Set<String> currentKeys = fingerprints(current.violations());

    List<SnapshotViolation> newViolations = new ArrayList<>();
    for (SnapshotViolation violation : current.violations()) {
      if (!baselineKeys.contains(violation.fingerprint())) {
        newViolations.add(violation);
      }
    }

    List<SnapshotViolation> resolvedViolations = new ArrayList<>();
    for (SnapshotViolation violation : baseline.violations()) {
      if (!currentKeys.contains(violation.fingerprint())) {
        resolvedViolations.add(violation);
      }
    }

    int scoreDelta = current.healthScore() - baseline.healthScore();

    return new RunComparison(baseline, current, scoreDelta, newViolations, resolvedViolations);
  }

  private static Set<String> fingerprints(List<SnapshotViolation> violations) {
    Set<String> keys = new HashSet<>();
    for (SnapshotViolation violation : violations) {
      keys.add(violation.fingerprint());
    }
    return keys;
  }
}
