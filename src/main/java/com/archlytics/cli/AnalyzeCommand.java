package com.archlytics.cli;

import com.archlytics.ai.AiAnalysisResult;
import com.archlytics.ai.AiAnalyzer;
import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphBuilder;
import com.archlytics.ingest.FileScanner;
import com.archlytics.ingest.ScannedFile;
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
    version = "0.1.0",
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
      description = "Skip Gemini analysis (graph and rules only)")
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
    List<Violation> violations = RuleEngine.analyze(graph);

    System.out.println("Archlytics — Architecture analysis");
    System.out.println("Repository: " + absoluteRepo);
    System.out.println("Java files: " + files.size());
    System.out.println("Modules: " + graph.modules().size());
    System.out.println("Violations: " + violations.size());
    System.out.println();

    if (skipAi) {
      printGraphAndViolations(graph, violations);
      return 0;
    }

    System.out.println("Calling Gemini for architecture analysis...");
    AiAnalysisResult ai =
        AiAnalyzer.analyze(absoluteRepo.toString(), graph, violations, files.size());

    String report =
        MarkdownReport.render(
            absoluteRepo.toString(), files.size(), graph, violations, ai);

    Path reportPath = outputPath.toAbsolutePath().normalize();
    Files.writeString(reportPath, report);

    System.out.println();
    System.out.println("Architecture type: " + ai.architectureType());
    System.out.println();
    System.out.println("Summary:");
    System.out.println(ai.summary());
    System.out.println();
    printGraphAndViolations(graph, violations);
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
}
