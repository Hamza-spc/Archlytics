package com.archlytics.cli;

import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphBuilder;
import com.archlytics.ingest.FileScanner;
import com.archlytics.ingest.ScannedFile;
import com.archlytics.rules.RuleEngine;
import com.archlytics.rules.Violation;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
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

  @Override
  public Integer call() {
    Path absoluteRepo = repoPath.toAbsolutePath().normalize();

    if (!java.nio.file.Files.isDirectory(absoluteRepo)) {
      System.err.println("Error: not a directory — " + absoluteRepo);
      return 1;
    }

    List<ScannedFile> files = FileScanner.scan(absoluteRepo);
    DependencyGraph graph = GraphBuilder.build(files);
    List<Violation> violations = RuleEngine.analyze(graph);

    System.out.println("Archlytics — Phase 3: Architecture rules");
    System.out.println("Repository: " + absoluteRepo);
    System.out.println("Java files: " + files.size());
    System.out.println("Modules: " + graph.modules().size());
    System.out.println("Violations: " + violations.size());
    System.out.println();

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
        System.out.printf("         rule: %s%n", violation.rule());
      }
    }

    return 0;
  }
}
