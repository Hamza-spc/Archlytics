package com.archlytics.ingest;

public final class JavaSources {

  private JavaSources() {}

  public static String read(ScannedFile file) {
    try {
      return file.readSource();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to read " + file.relativePath(), e);
    }
  }
}
