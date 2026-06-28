package com.archlytics.report;

import com.archlytics.config.ArchlyticsConfig;
import com.archlytics.graph.GraphMetrics;
import com.archlytics.rules.Severity;
import com.archlytics.rules.Violation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HealthScoreCalculator {

  static final int HIGH_PENALTY = 15;
  static final int MEDIUM_PENALTY = 8;
  static final int LOW_PENALTY = 3;
  static final int CIRCULAR_DEPENDENCY_PENALTY = 5;
  static final int DEEP_CHAIN_PENALTY = 5;

  private HealthScoreCalculator() {}

  public static HealthScore calculate(
      List<Violation> violations, GraphMetrics.Metrics metrics, ArchlyticsConfig config) {
    int score = 100;

    for (Violation violation : violations) {
      score -= penaltyFor(violation.severity());
    }

    if (hasCircularDependency(violations)) {
      score -= CIRCULAR_DEPENDENCY_PENALTY;
    }

    if (metrics.longestChain().size() >= config.rules.systemDesign.serialChainThreshold) {
      score -= DEEP_CHAIN_PENALTY;
    }

    score = Math.max(0, Math.min(100, score));

    return new HealthScore(score, labelFor(score), topRisks(violations));
  }

  private static int penaltyFor(Severity severity) {
    return switch (severity) {
      case HIGH -> HIGH_PENALTY;
      case MEDIUM -> MEDIUM_PENALTY;
      case LOW -> LOW_PENALTY;
    };
  }

  private static boolean hasCircularDependency(List<Violation> violations) {
    return violations.stream().anyMatch(v -> "circular-dependency".equals(v.rule()));
  }

  static String labelFor(int score) {
    if (score >= 80) {
      return "Healthy";
    }
    if (score >= 60) {
      return "Needs attention";
    }
    return "Critical";
  }

  private static List<String> topRisks(List<Violation> violations) {
    return violations.stream()
        .sorted(
            Comparator.comparing(Violation::severity)
                .thenComparing(Violation::title))
        .limit(3)
        .map(v -> v.title() + " — " + v.evidence())
        .toList();
  }
}
