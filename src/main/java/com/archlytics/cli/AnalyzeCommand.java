package com.archlytics.cli;

import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphBuilder;
import com.archlytics.ingest.FileScanner;
import com.archlytics.ingest.ScannedFile;
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

    System.out.println("Archlytics — Phase 2: Dependency graph");
    System.out.println("Repository: " + absoluteRepo);
    System.out.println("Java files: " + files.size());
    System.out.println("Internal file edges: " + graph.fileDependencies().size());
    System.out.println();

    System.out.println("Modules:");
    for (Map.Entry<String, DependencyGraph.ModuleInfo> entry : graph.modules().entrySet()) {
      DependencyGraph.ModuleInfo info = entry.getValue();
      System.out.printf(
          "  %s (%d files)%n", entry.getKey(), info.fileCount());
      if (!info.dependsOn().isEmpty()) {
        System.out.println("    depends on → " + String.join(", ", info.dependsOn()));
      }
      if (!info.usedBy().isEmpty()) {
        System.out.println("    used by    ← " + String.join(", ", info.usedBy()));
      }
    }

    System.out.println();
    System.out.println("Module dependency map:");
    for (Map.Entry<String, Set<String>> entry : graph.moduleDependencies().entrySet()) {
      if (!entry.getValue().isEmpty()) {
        System.out.println("  " + entry.getKey() + " → " + String.join(", ", entry.getValue()));
      }
    }

    System.out.println();
    System.out.println("Sample file dependencies (first 5):");
    graph.fileDependencies().entrySet().stream()
        .limit(5)
        .forEach(
            e ->
                System.out.println(
                    "  "
                        + e.getKey()
                        + " → "
                        + e.getValue().stream()
                            .map(Path::toString)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("")));

    return 0;
  }
}
