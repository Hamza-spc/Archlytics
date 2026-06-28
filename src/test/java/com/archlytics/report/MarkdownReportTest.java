package com.archlytics.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.ai.AiAnalysisResult;
import com.archlytics.graph.DependencyGraph;
import com.archlytics.rules.Severity;
import com.archlytics.rules.Violation;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarkdownReportTest {

  @Test
  void render_includesSummaryViolationsAndMermaid() {
    DependencyGraph graph =
        new DependencyGraph(
            Map.of(),
            Map.of("api", Set.of("core")),
            Map.of(
                "api",
                new DependencyGraph.ModuleInfo(3, Set.of("core"), Set.of()),
                "core",
                new DependencyGraph.ModuleInfo(5, Set.of(), Set.of("api"))));

    List<Violation> violations =
        List.of(
            new Violation(
                Severity.LOW,
                "high-coupling",
                "File has many internal dependencies",
                "App.java imports 8 internal types"));

    AiAnalysisResult ai =
        new AiAnalysisResult(
            "Layered monolith",
            "Clean separation between API and core.",
            List.of(new AiAnalysisResult.Recommendation("Split controller", "Extract DTO mapping")),
            "graph TD\n  api-->core");

    String report =
        MarkdownReport.render("/repo", 8, graph, violations, ai);

    assertTrue(report.contains("# Archlytics Architecture Report"));
    assertTrue(report.contains("Layered monolith"));
    assertTrue(report.contains("App.java imports 8 internal types"));
    assertTrue(report.contains("Split controller"));
    assertTrue(report.contains("```mermaid"));
    assertTrue(report.contains("api-->core"));
  }
}
