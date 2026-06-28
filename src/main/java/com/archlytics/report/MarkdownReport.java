package com.archlytics.report;

import com.archlytics.ai.AiAnalysisResult;
import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphMetrics;
import com.archlytics.rules.Violation;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MarkdownReport {

  private MarkdownReport() {}

  public static String render(
      String repoPath,
      int fileCount,
      DependencyGraph graph,
      GraphMetrics.Metrics metrics,
      List<Violation> violations,
      ArchitectureDiagrams diagrams,
      HealthScore healthScore,
      AiAnalysisResult ai) {
    StringBuilder md = new StringBuilder();

    md.append("# Archlytics Architecture Report\n\n");
    appendHealthScore(md, healthScore);
    md.append("**Repository:** `").append(repoPath).append("`\n\n");
    md.append("**Generated:** ")
        .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
        .append("\n\n");
    md.append("**Java files:** ").append(fileCount).append("\n\n");
    md.append("**Architecture type:** ").append(ai.architectureType()).append("\n\n");

    md.append("## Summary\n\n").append(ai.summary()).append("\n\n");

    appendGraphMetrics(md, metrics, graph);
    appendModuleDependencies(md, graph);
    appendViolations(md, violations);
    appendSystemDesignIssues(md, violations, ai);
    appendScalingRisks(md, ai);
    appendRecommendations(md, ai);
    appendDiagrams(md, diagrams, ai);

    return md.toString();
  }

  public static String renderWithoutAi(
      String repoPath,
      int fileCount,
      DependencyGraph graph,
      GraphMetrics.Metrics metrics,
      List<Violation> violations,
      ArchitectureDiagrams diagrams,
      HealthScore healthScore) {
    StringBuilder md = new StringBuilder();

    md.append("# Archlytics Architecture Report\n\n");
    appendHealthScore(md, healthScore);
    md.append("**Repository:** `").append(repoPath).append("`\n\n");
    md.append("**Generated:** ")
        .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
        .append("\n\n");
    md.append("**Java files:** ").append(fileCount).append("\n\n");
    md.append("_AI analysis skipped (`--skip-ai`). Deterministic analysis only._\n\n");

    appendGraphMetrics(md, metrics, graph);
    appendModuleDependencies(md, graph);
    appendViolations(md, violations);
    appendSystemDesignIssues(md, violations, null);
    appendDiagrams(md, diagrams, null);

    return md.toString();
  }

  private static void appendHealthScore(StringBuilder md, HealthScore healthScore) {
    md.append("## Architecture Health\n\n");
    md.append("**Score: ")
        .append(healthScore.score())
        .append("/100** — ")
        .append(healthScore.label())
        .append("\n\n");

    if (!healthScore.topRisks().isEmpty()) {
      md.append("### Top risks\n\n");
      int index = 1;
      for (String risk : healthScore.topRisks()) {
        md.append(index++).append(". ").append(risk).append("\n");
      }
      md.append("\n");
    }
  }

  private static void appendGraphMetrics(
      StringBuilder md, GraphMetrics.Metrics metrics, DependencyGraph graph) {
    md.append("## Graph Metrics\n\n");
    md.append("- **Longest dependency chain:** ")
        .append(
            metrics.longestChain().isEmpty()
                ? "_none_"
                : String.join(" → ", metrics.longestChain()))
        .append("\n");
    md.append("- **Entry points:** ")
        .append(
            metrics.entryPoints().isEmpty()
                ? "_none_"
                : String.join(", ", metrics.entryPoints()))
        .append("\n");
    md.append("- **Shared-kernel hubs:** ")
        .append(
            metrics.hubModules().isEmpty() ? "_none_" : String.join(", ", metrics.hubModules()))
        .append("\n");

    List<String> bypasses = GraphMetrics.findLayerBypasses(graph);
    md.append("- **Layer bypasses:** ")
        .append(bypasses.isEmpty() ? "_none_" : String.join("; ", bypasses))
        .append("\n\n");
  }

  private static void appendModuleDependencies(StringBuilder md, DependencyGraph graph) {
    md.append("## Module Dependencies\n\n");
    for (Map.Entry<String, Set<String>> entry : graph.moduleDependencies().entrySet()) {
      if (!entry.getValue().isEmpty()) {
        md.append("- **")
            .append(entry.getKey())
            .append("** → ")
            .append(String.join(", ", entry.getValue()))
            .append('\n');
      }
    }
    md.append('\n');
  }

  private static void appendViolations(StringBuilder md, List<Violation> violations) {
    md.append("## Violations (deterministic rules)\n\n");
    if (violations.isEmpty()) {
      md.append("_No violations detected._\n\n");
    } else {
      for (Violation violation : violations) {
        md.append("- **[")
            .append(violation.severity())
            .append("]** ")
            .append(violation.title())
            .append(" — ")
            .append(violation.evidence())
            .append(" _(rule: ")
            .append(violation.rule())
            .append(")_\n");
      }
      md.append('\n');
    }
  }

  private static void appendSystemDesignIssues(
      StringBuilder md, List<Violation> violations, AiAnalysisResult ai) {
    md.append("## System Design Issues\n\n");

    List<Violation> structural =
        violations.stream().filter(v -> "system-design".equals(v.rule())).toList();

    if (!structural.isEmpty()) {
      md.append("### From graph analysis\n\n");
      for (Violation violation : structural) {
        md.append("- **[")
            .append(violation.severity())
            .append("]** ")
            .append(violation.title())
            .append(" — ")
            .append(violation.evidence())
            .append("\n");
      }
      md.append('\n');
    }

    if (ai != null && !ai.systemDesignIssues().isEmpty()) {
      md.append("### From AI interpretation\n\n");
      for (AiAnalysisResult.SystemDesignIssue issue : ai.systemDesignIssues()) {
        md.append("- **[")
            .append(issue.impact())
            .append("]** ")
            .append(issue.title())
            .append(" — ")
            .append(issue.detail())
            .append("\n");
      }
      md.append('\n');
    } else if (structural.isEmpty()) {
      md.append("_No major structural issues detected._\n\n");
    }
  }

  private static void appendScalingRisks(StringBuilder md, AiAnalysisResult ai) {
    md.append("## Scaling & Bottleneck Analysis\n\n");
    if (ai == null || ai.scalingRisks().isEmpty()) {
      md.append("_Run without `--skip-ai` for AI scaling predictions._\n\n");
      return;
    }

    for (AiAnalysisResult.ScalingRisk risk : ai.scalingRisks()) {
      md.append("### ").append(risk.scenario()).append("\n\n");
      md.append("- **Risk:** ").append(risk.risk()).append("\n");
      md.append("- **Mitigation:** ").append(risk.mitigation()).append("\n\n");
    }
  }

  private static void appendRecommendations(StringBuilder md, AiAnalysisResult ai) {
    if (ai == null || ai.recommendations().isEmpty()) {
      return;
    }

    md.append("## AI Recommendations\n\n");
    for (AiAnalysisResult.Recommendation rec : ai.recommendations()) {
      md.append("### ").append(rec.title()).append("\n\n");
      md.append(rec.detail()).append("\n\n");
    }
  }

  private static void appendDiagrams(
      StringBuilder md, ArchitectureDiagrams diagrams, AiAnalysisResult ai) {
    md.append("## Architecture Diagrams\n\n");

    md.append("### Module Dependencies\n\n");
    md.append("```mermaid\n").append(diagrams.moduleDependency()).append("\n```\n\n");

    md.append("### Layer View\n\n");
    md.append("```mermaid\n").append(diagrams.layerView()).append("\n```\n\n");

    md.append("### Critical Path (longest chain)\n\n");
    md.append("```mermaid\n").append(diagrams.criticalPath()).append("\n```\n\n");

    if (ai != null && ai.mermaidDiagram() != null && !ai.mermaidDiagram().isBlank()) {
      md.append("### AI-Generated View\n\n");
      md.append("```mermaid\n").append(ai.mermaidDiagram()).append("\n```\n");
    }
  }
}
