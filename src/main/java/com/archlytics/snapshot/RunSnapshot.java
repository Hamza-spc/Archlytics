package com.archlytics.snapshot;

import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphMetrics;
import com.archlytics.report.HealthScore;
import com.archlytics.rules.Violation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record RunSnapshot(
    String capturedAt,
    String repository,
    int healthScore,
    String healthLabel,
    int javaFileCount,
    int moduleCount,
    int violationCount,
    List<String> longestChain,
    Map<String, List<String>> moduleDependencies,
    List<SnapshotViolation> violations) {

  public static RunSnapshot capture(
      String repository,
      int fileCount,
      DependencyGraph graph,
      GraphMetrics.Metrics metrics,
      HealthScore healthScore,
      List<Violation> violations,
      String capturedAt) {
    Map<String, List<String>> deps = new LinkedHashMap<>();
    for (Map.Entry<String, Set<String>> entry : graph.moduleDependencies().entrySet()) {
      if (!entry.getValue().isEmpty()) {
        deps.put(entry.getKey(), List.copyOf(entry.getValue()));
      }
    }

    return new RunSnapshot(
        capturedAt,
        repository,
        healthScore.score(),
        healthScore.label(),
        fileCount,
        graph.modules().size(),
        violations.size(),
        List.copyOf(metrics.longestChain()),
        deps,
        violations.stream().map(SnapshotViolation::from).toList());
  }
}
