package com.archlytics.cli;

import com.archlytics.ai.AiAnalysisResult;
import com.archlytics.ai.AiAnalyzer;
import com.archlytics.config.ArchlyticsConfig;
import com.archlytics.config.ConfigLoader;
import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphBuilder;
import com.archlytics.graph.GraphMetrics;
import com.archlytics.ingest.FileScanner;
import com.archlytics.ingest.ScannedFile;
import com.archlytics.report.ArchitectureDiagrams;
import com.archlytics.report.DiagramGenerator;
import com.archlytics.report.HealthScore;
import com.archlytics.report.HealthScoreCalculator;
import com.archlytics.report.HtmlReport;
import com.archlytics.report.MarkdownReport;
import com.archlytics.report.ReportFormat;
import com.archlytics.report.ReportWriter;
import com.archlytics.rules.RuleEngine;
import com.archlytics.rules.Violation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(
    name = "archlytics",
    mixinStandardHelpOptions = true,
    version = "0.4.0",
    description = "Analyze a Java repository and infer its architecture.")
public class AnalyzeCommand implements Callable<Integer> {

  @Parameters(
      index = "0",
      description = "Path to the repository to analyze",
      defaultValue = ".")
  Path repoPath;

  @Option(
      names = {"-o", "--output"},
      description = "Report output path (default: archlytics-report.md or .html by format)",
      defaultValue = "archlytics-report.md")
  Path outputPath;

  @Option(
      names = {"-f", "--format"},
      description = "Report format: md, html, or both",
      defaultValue = "md")
  String format;

  @Option(
      names = "--config",
      description = "Path to archlytics.yaml (default: <repo>/archlytics.yaml)")
  Path configPath;

  @Option(
      names = "--skip-ai",
      description = "Skip AI analysis (graph, rules, and diagrams only)")
  boolean skipAi;

  @Override
  public Integer call() throws IOException {
    Path absoluteRepo = repoPath.toAbsolutePath().normalize();
    ReportFormat reportFormat = ReportFormat.fromString(format);

    if (!Files.isDirectory(absoluteRepo)) {
      System.err.println("Error: not a directory — " + absoluteRepo);
      return 1;
    }

    ArchlyticsConfig config = ConfigLoader.load(absoluteRepo, configPath);
    boolean runAi = config.ai.enabled && !skipAi;

    List<ScannedFile> files = FileScanner.scan(absoluteRepo, config);
    DependencyGraph graph = GraphBuilder.build(files);
    GraphMetrics.Metrics metrics =
        GraphMetrics.compute(graph, config.rules.systemDesign.hubFanInThreshold);
    List<Violation> violations = RuleEngine.analyze(graph, config);
    ArchitectureDiagrams diagrams = DiagramGenerator.generate(graph);
    HealthScore healthScore = HealthScoreCalculator.calculate(violations, metrics, config);

    System.out.println("Archlytics — Architecture analysis");
    System.out.println("Repository: " + absoluteRepo);
    printConfigSource(absoluteRepo, configPath);
    System.out.println("Architecture health: " + healthScore.score() + "/100 — " + healthScore.label());
    System.out.println("Java files: " + files.size());
    System.out.println("Modules: " + graph.modules().size());
    System.out.println("Violations: " + violations.size());
    System.out.println(
        "Longest chain: "
            + (metrics.longestChain().isEmpty()
                ? "none"
                : String.join(" → ", metrics.longestChain())));
    System.out.println();

    AiAnalysisResult ai = null;
    if (runAi) {
      System.out.println(
          "Calling AI (" + AiAnalyzer.configuredProvider(config).name().toLowerCase() + ")...");
      ai =
          AiAnalyzer.analyze(
              absoluteRepo.toString(), graph, metrics, violations, files.size(), config);
    }

    List<Path> writtenReports = writeReports(absoluteRepo, files.size(), graph, metrics, violations, diagrams, healthScore, ai, runAi, reportFormat);

    if (ai != null) {
      System.out.println();
      System.out.println("Architecture type: " + ai.architectureType());
      System.out.println();
      System.out.println("Summary:");
      System.out.println(ai.summary());
    }

    System.out.println();
    printGraphAndViolations(graph, violations);
    if (ai != null) {
      printSystemDesignHighlights(violations, ai);
    }
    System.out.println();
    for (Path path : writtenReports) {
      System.out.println("Report written to: " + path);
    }

    return 0;
  }

  private List<Path> writeReports(
      Path absoluteRepo,
      int fileCount,
      DependencyGraph graph,
      GraphMetrics.Metrics metrics,
      List<Violation> violations,
      ArchitectureDiagrams diagrams,
      HealthScore healthScore,
      AiAnalysisResult ai,
      boolean runAi,
      ReportFormat reportFormat)
      throws IOException {
    List<Path> written = new ArrayList<>();
    Path mdPath = outputPath.toAbsolutePath().normalize();
    Path htmlPath = ReportWriter.htmlPathFor(mdPath);

    if (reportFormat == ReportFormat.MD || reportFormat == ReportFormat.BOTH) {
      String markdown =
          runAi
              ? MarkdownReport.render(
                  absoluteRepo.toString(), fileCount, graph, metrics, violations, diagrams, healthScore, ai)
              : MarkdownReport.renderWithoutAi(
                  absoluteRepo.toString(), fileCount, graph, metrics, violations, diagrams, healthScore);
      Files.writeString(mdPath, markdown);
      written.add(mdPath);
    }

    if (reportFormat == ReportFormat.HTML || reportFormat == ReportFormat.BOTH) {
      String html =
          runAi
              ? HtmlReport.render(
                  absoluteRepo.toString(), fileCount, graph, metrics, violations, diagrams, healthScore, ai)
              : HtmlReport.renderWithoutAi(
                  absoluteRepo.toString(), fileCount, graph, metrics, violations, diagrams, healthScore);
      Files.writeString(htmlPath, html);
      written.add(htmlPath);
    }

    return written;
  }

  private static void printConfigSource(Path repoRoot, Path explicitConfig) {
    Optional<Path> configFile = ConfigLoader.resolveConfigFile(repoRoot, explicitConfig);
    if (configFile.isPresent()) {
      System.out.println("Config: " + configFile.get());
    } else {
      System.out.println("Config: defaults (no archlytics.yaml found)");
    }
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
