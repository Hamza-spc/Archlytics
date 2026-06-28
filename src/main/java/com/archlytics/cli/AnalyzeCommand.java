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
import com.archlytics.pr.PullRequestAnalysis;
import com.archlytics.snapshot.RunComparison;
import com.archlytics.snapshot.RunSnapshot;
import com.archlytics.snapshot.SnapshotComparer;
import com.archlytics.snapshot.SnapshotStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    version = "0.6.0",
    description = "Analyze a Java repository and infer its architecture.")
public class AnalyzeCommand implements Callable<Integer> {

  private static final DateTimeFormatter CAPTURED_AT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

  @Option(
      names = "--save-snapshot",
      description = "Save analysis snapshot to <repo>/.archlytics/snapshots/")
  boolean saveSnapshot;

  @Option(
      names = "--compare",
      description = "Compare with a snapshot file, or 'latest' for the most recent snapshot",
      paramLabel = "SNAPSHOT")
  String compareTarget;

  @Option(
      names = "--base",
      description = "PR analysis: base git ref (e.g. origin/main)")
  String baseRef;

  @Option(
      names = "--head",
      description = "PR analysis: head git ref (default: HEAD)")
  String headRef;

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

    RunSnapshot currentSnapshot =
        RunSnapshot.capture(
            absoluteRepo.toString(),
            files.size(),
            graph,
            metrics,
            healthScore,
            violations,
            CAPTURED_AT.format(LocalDateTime.now()));

    RunComparison comparison = null;
    if (compareTarget != null) {
      Path baselinePath = SnapshotStore.resolveCompareTarget(absoluteRepo, compareTarget);
      RunSnapshot baseline = SnapshotStore.load(baselinePath);
      comparison = SnapshotComparer.compare(baseline, currentSnapshot);
    }

    PullRequestAnalysis pullRequestAnalysis = null;
    if (baseRef != null && !baseRef.isBlank()) {
      String head = headRef == null || headRef.isBlank() ? "HEAD" : headRef;
      pullRequestAnalysis = PullRequestAnalysis.analyze(absoluteRepo, baseRef, head, config);
    }

    System.out.println("Archlytics — Architecture analysis");
    System.out.println("Repository: " + absoluteRepo);
    printConfigSource(absoluteRepo, configPath);
    System.out.println("Architecture health: " + healthScore.score() + "/100 — " + healthScore.label());
    if (comparison != null) {
      System.out.println("Health drift: " + comparison.scoreSummary());
      System.out.println(
          "Changes: +"
              + comparison.newViolations().size()
              + " new, -"
              + comparison.resolvedViolations().size()
              + " resolved");
    }
    if (pullRequestAnalysis != null) {
      System.out.println(
          "PR analysis: "
              + pullRequestAnalysis.baseRef()
              + " → "
              + pullRequestAnalysis.headRef()
              + " ("
              + pullRequestAnalysis.changedFiles().size()
              + " files changed)");
      System.out.println(
          "PR violations introduced: " + pullRequestAnalysis.introducedViolations().size());
      if (!pullRequestAnalysis.newModuleEdges().isEmpty()) {
        System.out.println("PR new module edges: " + pullRequestAnalysis.newModuleEdges().size());
      }
    }
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

    List<Path> writtenReports =
        writeReports(
            absoluteRepo,
            files.size(),
            graph,
            metrics,
            violations,
            diagrams,
            healthScore,
            comparison,
            pullRequestAnalysis,
            ai,
            runAi,
            reportFormat);

    if (saveSnapshot) {
      Path snapshotPath = SnapshotStore.save(absoluteRepo, currentSnapshot);
      System.out.println("Snapshot saved to: " + snapshotPath);
    }

    if (ai != null) {
      System.out.println();
      System.out.println("Architecture type: " + ai.architectureType());
      System.out.println();
      System.out.println("Summary:");
      System.out.println(ai.summary());
    }

    System.out.println();
    printGraphAndViolations(graph, violations);
    if (comparison != null) {
      printComparisonDetails(comparison);
    }
    if (pullRequestAnalysis != null) {
      printPullRequestDetails(pullRequestAnalysis);
    }
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
      RunComparison comparison,
      PullRequestAnalysis pullRequestAnalysis,
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
                  absoluteRepo.toString(),
                  fileCount,
                  graph,
                  metrics,
                  violations,
                  diagrams,
                  healthScore,
                  comparison,
                  pullRequestAnalysis,
                  ai)
              : MarkdownReport.renderWithoutAi(
                  absoluteRepo.toString(),
                  fileCount,
                  graph,
                  metrics,
                  violations,
                  diagrams,
                  healthScore,
                  comparison,
                  pullRequestAnalysis);
      Files.writeString(mdPath, markdown);
      written.add(mdPath);
    }

    if (reportFormat == ReportFormat.HTML || reportFormat == ReportFormat.BOTH) {
      String html =
          runAi
              ? HtmlReport.render(
                  absoluteRepo.toString(),
                  fileCount,
                  graph,
                  metrics,
                  violations,
                  diagrams,
                  healthScore,
                  comparison,
                  pullRequestAnalysis,
                  ai)
              : HtmlReport.renderWithoutAi(
                  absoluteRepo.toString(),
                  fileCount,
                  graph,
                  metrics,
                  violations,
                  diagrams,
                  healthScore,
                  comparison,
                  pullRequestAnalysis);
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

  private static void printComparisonDetails(RunComparison comparison) {
    System.out.println();
    if (!comparison.newViolations().isEmpty()) {
      System.out.println("New violations:");
      comparison.newViolations().forEach(v -> System.out.println("  + " + v));
    }
    if (!comparison.resolvedViolations().isEmpty()) {
      System.out.println("Resolved violations:");
      comparison.resolvedViolations().forEach(v -> System.out.println("  - " + v));
    }
  }

  private static void printPullRequestDetails(PullRequestAnalysis pr) {
    System.out.println();
    System.out.println("PR changed files:");
    pr.changedFiles().forEach(path -> System.out.println("  * " + path));

    if (!pr.newModuleEdges().isEmpty()) {
      System.out.println("New module dependencies:");
      pr.newModuleEdges().forEach(edge -> System.out.println("  + " + edge));
    }

    if (!pr.introducedViolations().isEmpty()) {
      System.out.println("Violations introduced by PR:");
      for (Violation violation : pr.introducedViolations()) {
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
