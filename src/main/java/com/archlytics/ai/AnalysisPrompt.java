package com.archlytics.ai;

import com.archlytics.config.ArchlyticsConfig;
import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphMetrics;
import com.archlytics.rules.Violation;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AnalysisPrompt {

  private AnalysisPrompt() {}

  public static String build(
      String repoPath,
      DependencyGraph graph,
      GraphMetrics.Metrics metrics,
      List<Violation> violations,
      int fileCount,
      ArchlyticsConfig config) {
    StringBuilder prompt = new StringBuilder();
    prompt.append(
        """
        You are a senior software architect analyzing a Java codebase.
        Use ONLY the facts below. Do not invent modules or dependencies.

        Return JSON with this exact shape:
        {
          "architectureType": "short label, e.g. Layered modular monolith",
          "summary": "2-4 sentences describing the architecture",
          "recommendations": [
            {"title": "short title", "detail": "actionable suggestion"}
          ],
          "systemDesignIssues": [
            {"title": "issue name", "impact": "HIGH|MEDIUM|LOW", "detail": "why it matters"}
          ],
          "scalingRisks": [
            {"scenario": "e.g. 10x traffic", "risk": "what breaks", "mitigation": "what to do"}
          ],
          "mermaidDiagram": "graph TD\\n  moduleA-->moduleB"
        }

        Rules for mermaidDiagram:
        - Use graph TD
        - Node IDs must be alphanumeric (no spaces), use labels like moduleA[maroctax-api]
        - Include all modules and cross-module edges
        - Max 15 lines

        Provide:
        - 2-5 recommendations based on violations and structure
        - 2-4 systemDesignIssues focused on structural/design problems (bottlenecks, coupling, layering)
        - 2-3 scalingRisks predicting what fails as load grows (use graph metrics, not runtime data)
        """);

    prompt.append("\nRepository: ").append(repoPath).append('\n');
    prompt.append("Java files: ").append(fileCount).append('\n');
    prompt.append("\nGraph metrics:\n")
        .append(
            GraphMetrics.toSummary(
                metrics, graph, config.rules.systemDesign.hubFanInThreshold));

    prompt.append("\nModules:\n");
    for (Map.Entry<String, DependencyGraph.ModuleInfo> entry : graph.modules().entrySet()) {
      DependencyGraph.ModuleInfo info = entry.getValue();
      prompt.append("- ")
          .append(entry.getKey())
          .append(" (")
          .append(info.fileCount())
          .append(" files)");
      if (!info.dependsOn().isEmpty()) {
        prompt.append(", depends on: ").append(String.join(", ", info.dependsOn()));
      }
      if (!info.usedBy().isEmpty()) {
        prompt.append(", used by: ").append(String.join(", ", info.usedBy()));
      }
      prompt.append('\n');
    }

    prompt.append("\nModule dependency edges:\n");
    for (Map.Entry<String, Set<String>> entry : graph.moduleDependencies().entrySet()) {
      if (!entry.getValue().isEmpty()) {
        prompt.append("- ")
            .append(entry.getKey())
            .append(" -> ")
            .append(String.join(", ", entry.getValue()))
            .append('\n');
      }
    }

    prompt.append("\nDeterministic violations already detected:\n");
    if (violations.isEmpty()) {
      prompt.append("- none\n");
    } else {
      for (Violation violation : violations) {
        prompt.append("- [")
            .append(violation.severity())
            .append("] ")
            .append(violation.title())
            .append(": ")
            .append(violation.evidence())
            .append('\n');
      }
    }

    return prompt.toString();
  }
}
