package com.archlytics.cli;

import com.archlytics.ai.AiAnalysisResult;
import com.archlytics.ai.AiAnalyzer;
import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphBuilder;
import com.archlytics.graph.GraphMetrics;
import com.archlytics.ingest.FileScanner;
import com.archlytics.ingest.ScannedFile;
import com.archlytics.report.ArchitectureDiagrams;
import com.archlytics.report.DiagramGenerator;
import com.archlytics.report.MarkdownReport;
import com.archlytics.rules.RuleEngine;
import com.archlytics.rules.Violation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "archlytics",
    mixinStandardHelpOptions = true,
    version = "0.2.0",
    description = "Analyze a Java repository and infer its architecture.")
public class AnalyzeCommand implements Callable<Integer> {

  @Parameters(
      index = "0",
      description = "Path to the repository to analyze",
      defaultValue = ".")
  Path repoPath;

  @Option(
      names = {"-o", "--output"},
      description = "Markdown report output path",
      defaultValue = "archlytics-report.md")
  Path outputPath;

  @Option(
      names = "--skip-ai",
      description = "Skip Gemini analysis (graph, rules, and diagrams only)")
  boolean skipAi;

  @Override
  public Integer call() throws IOException {
    Path absoluteRepo = repoPath.toAbsolutePath().normalize();

    if (!Files.isDirectory(absoluteRepo)) {
      System.err.println("Error: not a directory — " + absoluteRepo);
      return 1;
    }

    List<ScannedFile> files = FileScanner.scan(absoluteRepo);
    DependencyGraph graph = GraphBuilder.build(files);
    GraphMetrics.Metrics metrics = GraphMetrics.compute(graph);
    List<Violation> violations = RuleEngine.analyze(graph);
    ArchitectureDiagrams diagrams = DiagramGenerator.generate(graph);

    System.out.println("Archlytics — Architecture analysis");
    System.out.println("Repository: " + absoluteRepo);
    System.out.println("Java files: " + files.size());
    System.out.println("Modules: " + graph.modules().size());
    System.out.println("Violations: " + violations.size());
    System.out.println(
        "Longest chain: "
            + (metrics.longestChain().isEmpty()
                ? "none"
                : String.join(" → ", metrics.longestChain())));
    System.out.println();

    Path reportPath = outputPath.toAbsolutePath().normalize();
    String report;

    if (skipAi) {
      report =
          MarkdownReport.renderWithoutAi(
              absoluteRepo.toString(), files.size(), graph, metrics, violations, diagrams);
      Files.writeString(reportPath, report);
      printGraphAndViolations(graph, violations);
      System.out.println();
      System.out.println("Report written to: " + reportPath);
      return 0;
    }

    System.out.println(
        "Calling AI (" + AiAnalyzer.configuredProvider().name().toLowerCase() + ")...");
    AiAnalysisResult ai =
        AiAnalyzer.analyze(absoluteRepo.toString(), graph, metrics, violations, files.size());

    report =
        MarkdownReport.render(
            absoluteRepo.toString(), files.size(), graph, metrics, violations, diagrams, ai);

    Files.writeString(reportPath, report);

    System.out.println();
    System.out.println("Architecture type: " + ai.architectureType());
    System.out.println();
    System.out.println("Summary:");
    System.out.println(ai.summary());
    System.out.println();
    printGraphAndViolations(graph, violations);
    printSystemDesignHighlights(violations, ai);
    System.out.println();
    System.out.println("Report written to: " + reportPath);

    return 0;
  }

  private static void printGraphAndViolations(DependencyGraph graph, List<Violation> violations) {
    System.out.println("Module dependency map:");
    for (Map.Entry<String, Set<String>> entry : graph.moduleDependencies().entrySet()) {
      if (!entry.getValue().isEmpty()) {
        System.out.println("  " + entry.getKey() + " → " + String.join(", ", entry.getValue()));
      }
    }

    System.out.println();
    if (violations.isEmpty()) {
      System.out.println("No violations detected.");
    } else {
      System.out.println("Violations:");
      for (Violation violation : violations) {
        System.out.printf(
            "  [%s] %s — %s%n", violation.severity(), violation.title(), violation.evidence());
      }
    }
  }

  private static void printSystemDesignHighlights(
      List<Violation> violations, AiAnalysisResult ai) {
    System.out.println();
    if (!ai.scalingRisks().isEmpty()) {
      System.out.println("Scaling risks:");
      for (AiAnalysisResult.ScalingRisk risk : ai.scalingRisks()) {
        System.out.printf("  %s — %s%n", risk.scenario(), risk.risk());
      }
    }

    long designIssues =
        violations.stream().filter(v -> "system-design".equals(v.rule())).count()
            + ai.systemDesignIssues().size();
    if (designIssues > 0) {
      System.out.println("System design issues found: " + designIssues);
    }
  }
}
