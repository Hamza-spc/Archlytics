package com.archlytics.report;

import java.nio.file.Path;

public final class ReportWriter {

  private ReportWriter() {}

  public static Path htmlPathFor(Path outputPath) {
    String fileName = outputPath.getFileName().toString();
    if (fileName.endsWith(".md")) {
      fileName = fileName.substring(0, fileName.length() - 3) + ".html";
    } else if (!fileName.endsWith(".html")) {
      fileName = fileName + ".html";
    }
    Path parent = outputPath.getParent();
    return parent == null ? Path.of(fileName) : parent.resolve(fileName);
  }
}
