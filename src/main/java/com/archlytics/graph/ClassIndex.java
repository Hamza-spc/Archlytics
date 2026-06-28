package com.archlytics.graph;

import com.archlytics.ingest.JavaSources;
import com.archlytics.ingest.ScannedFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ClassIndex {

  private final Map<String, ScannedFile> byFullyQualifiedName = new HashMap<>();

  public static ClassIndex build(Iterable<ScannedFile> files) {
    ClassIndex index = new ClassIndex();
    for (ScannedFile file : files) {
      index.register(file);
    }
    return index;
  }

  private void register(ScannedFile file) {
    String source = JavaSources.read(file);
    Optional<String> packageName = JavaSourceParser.parsePackage(source);
    if (packageName.isEmpty()) {
      return;
    }

    String className =
        JavaSourceParser.classNameFromFile(file.absolutePath().getFileName().toString());
    String fqn = packageName.get() + "." + className;
    byFullyQualifiedName.put(fqn, file);
  }

  public Optional<ScannedFile> find(String fullyQualifiedName) {
    return Optional.ofNullable(byFullyQualifiedName.get(fullyQualifiedName));
  }

  public int size() {
    return byFullyQualifiedName.size();
  }
}
