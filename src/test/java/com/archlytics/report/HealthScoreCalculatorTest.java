package com.archlytics.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.config.ArchlyticsConfig;
import com.archlytics.graph.GraphMetrics;
import com.archlytics.rules.Severity;
import com.archlytics.rules.Violation;
import java.util.List;
import org.junit.jupiter.api.Test;

class HealthScoreCalculatorTest {

  @Test
  void calculate_perfectScoreWhenNoViolations() {
    GraphMetrics.Metrics metrics = new GraphMetrics.Metrics(List.of("api"), List.of(), List.of("api"));

    HealthScore score =
        HealthScoreCalculator.calculate(List.of(), metrics, ArchlyticsConfig.defaults());

    assertEquals(100, score.score());
    assertEquals("Healthy", score.label());
  }

  @Test
  void calculate_deductsBySeverityAndDeepChain() {
    List<Violation> violations =
        List.of(
            new Violation(Severity.HIGH, "system-design", "Shared kernel bottleneck", "core hub"),
            new Violation(Severity.MEDIUM, "system-design", "Serial dependency chain", "a → b → c"),
            new Violation(Severity.MEDIUM, "system-design", "Layer bypass detected", "api → core"),
            new Violation(Severity.LOW, "high-coupling", "File has many internal dependencies", "App.java"),
            new Violation(Severity.LOW, "high-coupling", "File has many internal dependencies", "Service.java"),
            new Violation(Severity.LOW, "system-design", "Entry module fans out", "api fans out"));

    GraphMetrics.Metrics metrics =
        new GraphMetrics.Metrics(List.of("a", "b", "c"), List.of("b"), List.of("a"));

    HealthScore score =
        HealthScoreCalculator.calculate(violations, metrics, ArchlyticsConfig.defaults());

    assertEquals(55, score.score());
    assertEquals("Critical", score.label());
    assertTrue(score.topRisks().size() <= 3);
    assertTrue(score.topRisks().get(0).contains("Shared kernel bottleneck"));
  }

  @Test
  void labelFor_mapsScoreBands() {
    assertEquals("Healthy", HealthScoreCalculator.labelFor(85));
    assertEquals("Needs attention", HealthScoreCalculator.labelFor(72));
    assertEquals("Critical", HealthScoreCalculator.labelFor(45));
  }
}
