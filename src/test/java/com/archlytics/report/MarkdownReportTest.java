package com.archlytics.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.config.ArchlyticsConfig;
import com.archlytics.ai.AiAnalysisResult;
import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphMetrics;
import com.archlytics.rules.Severity;
import com.archlytics.rules.Violation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarkdownReportTest {

  @Test
  void render_includesDiagramsSystemDesignAndScaling() {
    DependencyGraph graph =
        new DependencyGraph(
            Map.of(),
            Map.of("api", Set.of("core")),
            Map.of(
                "api", new DependencyGraph.ModuleInfo(3, Set.of("core"), Set.of()),
                "core", new DependencyGraph.ModuleInfo(5, Set.of(), Set.of("api"))));

    GraphMetrics.Metrics metrics = GraphMetrics.compute(graph);
    ArchitectureDiagrams diagrams = DiagramGenerator.generate(graph);

    List<Violation> violations =
        List.of(
            new Violation(
                Severity.HIGH,
                "system-design",
                "Shared kernel bottleneck",
                "core is depended on by 2 modules"));

    HealthScore healthScore =
        HealthScoreCalculator.calculate(violations, metrics, ArchlyticsConfig.defaults());

    AiAnalysisResult ai =
        new AiAnalysisResult(
            "Layered monolith",
            "Clean separation between API and core.",
            List.of(new AiAnalysisResult.Recommendation("Split controller", "Extract DTO mapping")),
            List.of(
                new AiAnalysisResult.SystemDesignIssue(
                    "Centralized core", "HIGH", "Core module is a scaling choke point")),
            List.of(
                new AiAnalysisResult.ScalingRisk(
                    "10x API traffic", "Core CPU saturation", "Extract compute-heavy paths")),
            "graph TD\n  api-->core");

    String report =
        MarkdownReport.render("/repo", 8, graph, metrics, violations, diagrams, healthScore, null, ai);

    assertTrue(report.contains("## Architecture Health"));
    assertTrue(report.contains("/100"));
    assertTrue(report.contains("### Layer View"));
    assertTrue(report.contains("## System Design Issues"));
    assertTrue(report.contains("## Scaling & Bottleneck Analysis"));
    assertTrue(report.contains("10x API traffic"));
    assertTrue(report.contains("Shared kernel bottleneck"));
  }
}
