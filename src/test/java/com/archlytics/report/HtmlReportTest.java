package com.archlytics.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.config.ArchlyticsConfig;
import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphMetrics;
import com.archlytics.rules.Severity;
import com.archlytics.rules.Violation;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HtmlReportTest {

  @Test
  void render_includesHealthDashboardMermaidAndPrintButton() {
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

    String html =
        HtmlReport.renderWithoutAi(
            "/repo/path", 8, graph, metrics, violations, diagrams, healthScore, null, null);

    assertTrue(html.contains("<!DOCTYPE html>"));
    assertTrue(html.contains("Print / Save PDF"));
    assertTrue(html.contains("class=\"mermaid\""));
    assertTrue(html.contains("score-card"));
    assertTrue(html.contains("Shared kernel bottleneck"));
    assertTrue(html.contains("mermaid.min.js"));
  }

  @Test
  void escape_sanitizesHtml() {
    assertEquals("a &lt;b&gt; &amp; c", HtmlReport.escape("a <b> & c"));
  }

  @Test
  void htmlPathFor_swapsMdExtension() {
    assertEquals(
        Path.of("reports/archlytics-report.html"),
        ReportWriter.htmlPathFor(Path.of("reports/archlytics-report.md")));
  }
}
