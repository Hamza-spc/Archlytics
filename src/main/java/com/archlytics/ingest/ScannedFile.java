package com.archlytics.ingest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record ScannedFile(Path absolutePath, Path relativePath, String module, String sourceContent) {

  public ScannedFile(Path absolutePath, Path relativePath, String module) {
    this(absolutePath, relativePath, module, null);
  }

  public String readSource() throws IOException {
    if (sourceContent != null) {
      return sourceContent;
    }
    return Files.readString(absolutePath);
  }
}
