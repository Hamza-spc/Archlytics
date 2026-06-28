package com.archlytics.pr;

import com.archlytics.config.ArchlyticsConfig;
import com.archlytics.git.GitRevisionScanner;
import com.archlytics.git.GitRunner;
import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphBuilder;
import com.archlytics.graph.GraphMetrics;
import com.archlytics.ingest.FileScanner;
import com.archlytics.ingest.ScannedFile;
import com.archlytics.report.HealthScore;
import com.archlytics.report.HealthScoreCalculator;
import com.archlytics.rules.RuleEngine;
import com.archlytics.rules.Violation;
import com.archlytics.snapshot.RunComparison;
import com.archlytics.snapshot.RunSnapshot;
import com.archlytics.snapshot.SnapshotComparer;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record PullRequestAnalysis(
    String baseRef,
    String headRef,
    List<Path> changedFiles,
    Set<String> changedModules,
    List<String> newModuleEdges,
    List<Violation> introducedViolations,
    RunComparison violationComparison) {

  private static final DateTimeFormatter CAPTURED_AT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public static PullRequestAnalysis analyze(
      Path repoRoot, String baseRef, String headRef, ArchlyticsConfig config) {
    if (!GitRunner.isGitRepository(repoRoot)) {
      throw new IllegalStateException("PR analysis requires a git repository: " + repoRoot);
    }

    List<Path> changedFiles =
        GitRunner.changedJavaFiles(repoRoot, baseRef, headRef).stream()
            .map(Path::of)
            .sorted()
            .toList();

    Set<String> changedModules = new LinkedHashSet<>();
    for (Path changed : changedFiles) {
      changedModules.add(FileScanner.inferModule(changed));
    }

    List<ScannedFile> baseFiles = GitRevisionScanner.scanAtRef(repoRoot, baseRef, config);
    List<ScannedFile> headFiles = FileScanner.scan(repoRoot, config);

    DependencyGraph baseGraph = GraphBuilder.build(baseFiles);
    DependencyGraph headGraph = GraphBuilder.build(headFiles);

    GraphMetrics.Metrics baseMetrics =
        GraphMetrics.compute(baseGraph, config.rules.systemDesign.hubFanInThreshold);
    GraphMetrics.Metrics headMetrics =
        GraphMetrics.compute(headGraph, config.rules.systemDesign.hubFanInThreshold);

    List<Violation> baseViolations = RuleEngine.analyze(baseGraph, config);
    List<Violation> headViolations = RuleEngine.analyze(headGraph, config);

    HealthScore baseScore =
        HealthScoreCalculator.calculate(baseViolations, baseMetrics, config);
    HealthScore headScore =
        HealthScoreCalculator.calculate(headViolations, headMetrics, config);

    String capturedAt = CAPTURED_AT.format(LocalDateTime.now());
    RunSnapshot baseSnapshot =
        RunSnapshot.capture(
            repoRoot.toString(),
            baseFiles.size(),
            baseGraph,
            baseMetrics,
            baseScore,
            baseViolations,
            capturedAt);
    RunSnapshot headSnapshot =
        RunSnapshot.capture(
            repoRoot.toString(),
            headFiles.size(),
            headGraph,
            headMetrics,
            headScore,
            headViolations,
            capturedAt);

    RunComparison comparison = SnapshotComparer.compare(baseSnapshot, headSnapshot);
    List<String> newModuleEdges = findNewModuleEdges(baseGraph, headGraph);
    List<Violation> introduced =
        comparison.newViolations().stream().map(PullRequestAnalysis::toViolation).toList();

    return new PullRequestAnalysis(
        baseRef,
        headRef,
        changedFiles,
        changedModules,
        newModuleEdges,
        introduced,
        comparison);
  }

  private static List<String> findNewModuleEdges(
      DependencyGraph baseGraph, DependencyGraph headGraph) {
    Set<String> baseEdges = moduleEdgeKeys(baseGraph.moduleDependencies());
    List<String> newEdges = new ArrayList<>();

    for (Map.Entry<String, Set<String>> entry : headGraph.moduleDependencies().entrySet()) {
      for (String target : entry.getValue()) {
        String edge = entry.getKey() + " → " + target;
        if (!baseEdges.contains(edge)) {
          newEdges.add(edge);
        }
      }
    }

    return newEdges;
  }

  private static Set<String> moduleEdgeKeys(Map<String, Set<String>> moduleDependencies) {
    Set<String> edges = new LinkedHashSet<>();
    for (Map.Entry<String, Set<String>> entry : moduleDependencies.entrySet()) {
      for (String target : entry.getValue()) {
        edges.add(entry.getKey() + " → " + target);
      }
    }
    return edges;
  }

  private static Violation toViolation(com.archlytics.snapshot.SnapshotViolation snapshot) {
    return new Violation(
        com.archlytics.rules.Severity.valueOf(snapshot.severity()),
        snapshot.rule(),
        snapshot.title(),
        snapshot.evidence());
  }
}
