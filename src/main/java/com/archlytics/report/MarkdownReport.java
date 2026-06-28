package com.archlytics.report;

import com.archlytics.ai.AiAnalysisResult;
import com.archlytics.graph.DependencyGraph;
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
      List<Violation> violations,
      AiAnalysisResult ai) {
    StringBuilder md = new StringBuilder();

    md.append("# Archlytics Architecture Report\n\n");
    md.append("**Repository:** `").append(repoPath).append("`\n\n");
    md.append("**Generated:** ")
        .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
        .append("\n\n");
    md.append("**Java files:** ").append(fileCount).append("\n\n");
    md.append("**Architecture type:** ").append(ai.architectureType()).append("\n\n");

    md.append("## Summary\n\n").append(ai.summary()).append("\n\n");

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

    md.append("## AI Recommendations\n\n");
    for (AiAnalysisResult.Recommendation rec : ai.recommendations()) {
      md.append("### ").append(rec.title()).append("\n\n");
      md.append(rec.detail()).append("\n\n");
    }

    md.append("## Architecture Diagram\n\n");
    md.append("```mermaid\n").append(ai.mermaidDiagram()).append("\n```\n");

    return md.toString();
  }
}
