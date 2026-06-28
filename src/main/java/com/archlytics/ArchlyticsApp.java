package com.archlytics;

import com.archlytics.cli.AnalyzeCommand;
import picocli.CommandLine;

public final class ArchlyticsApp {

  public static void main(String[] args) {
    int exitCode = new CommandLine(new AnalyzeCommand()).execute(args);
    System.exit(exitCode);
  }

  private ArchlyticsApp() {}
}
