package com.archlytics.cli;

import com.archlytics.ingest.FileScanner;
import com.archlytics.ingest.ScannedFile;
import java.nio.file.Path;
import java.util.List;
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

    System.out.println("Archlytics — Phase 1: Repository scan");
    System.out.println("Repository: " + absoluteRepo);
    System.out.println("Java files found: " + files.size());
    System.out.println();

    for (ScannedFile file : files) {
      System.out.println("  " + file.relativePath());
    }

    return 0;
  }
}
