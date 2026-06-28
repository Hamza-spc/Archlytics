package com.archlytics.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JavaSourceParser {

  private static final Pattern PACKAGE =
      Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;", Pattern.MULTILINE);

  private static final Pattern IMPORT =
      Pattern.compile("^\\s*import\\s+(?!static)([\\w.]+)(?:\\.\\*)?\\s*;", Pattern.MULTILINE);

  private JavaSourceParser() {}

  public static Optional<String> parsePackage(String source) {
    Matcher matcher = PACKAGE.matcher(source);
    if (matcher.find()) {
      return Optional.of(matcher.group(1));
    }
    return Optional.empty();
  }

  /** Returns fully qualified type names; wildcard imports are omitted. */
  public static List<String> parseImports(String source) {
    List<String> imports = new ArrayList<>();
    Matcher matcher = IMPORT.matcher(source);
    while (matcher.find()) {
      String typeName = matcher.group(1);
      if (!typeName.endsWith(".*")) {
        imports.add(typeName);
      }
    }
    return imports;
  }

  public static String classNameFromFile(String fileName) {
    if (!fileName.endsWith(".java")) {
      throw new IllegalArgumentException("Not a Java file: " + fileName);
    }
    return fileName.substring(0, fileName.length() - ".java".length());
  }
}
