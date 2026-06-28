package com.archlytics.graph;

import com.archlytics.ingest.ScannedFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GraphBuilder {

  private GraphBuilder() {}

  public static DependencyGraph build(List<ScannedFile> files) {
    ClassIndex index = ClassIndex.build(files);
    Map<Path, Set<Path>> fileDependencies = new LinkedHashMap<>();

    for (ScannedFile file : files) {
      Set<Path> deps = resolveFileDependencies(file, index);
      if (!deps.isEmpty()) {
        fileDependencies.put(file.relativePath(), deps);
      }
    }

    Map<String, Set<String>> moduleDependencies = new LinkedHashMap<>();
    Map<String, ModuleInfoBuilder> moduleInfoBuilders = new HashMap<>();

    for (ScannedFile file : files) {
      moduleInfoBuilders
          .computeIfAbsent(file.module(), ModuleInfoBuilder::new)
          .incrementFileCount();
    }

    for (Map.Entry<Path, Set<Path>> entry : fileDependencies.entrySet()) {
      Path sourcePath = entry.getKey();
      String sourceModule = moduleForPath(sourcePath, files);
      Set<String> targets =
          moduleDependencies.computeIfAbsent(sourceModule, ignored -> new LinkedHashSet<>());

      for (Path targetPath : entry.getValue()) {
        String targetModule = moduleForPath(targetPath, files);
        if (!sourceModule.equals(targetModule)) {
          targets.add(targetModule);
          moduleInfoBuilders.get(sourceModule).dependsOn.add(targetModule);
          moduleInfoBuilders.get(targetModule).usedBy.add(sourceModule);
        }
      }
    }

    Map<String, DependencyGraph.ModuleInfo> modules = new LinkedHashMap<>();
    moduleInfoBuilders.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            e ->
                modules.put(
                    e.getKey(),
                    new DependencyGraph.ModuleInfo(
                        e.getValue().fileCount,
                        Set.copyOf(e.getValue().dependsOn),
                        Set.copyOf(e.getValue().usedBy))));

    return new DependencyGraph(fileDependencies, moduleDependencies, modules);
  }

  private static Set<Path> resolveFileDependencies(ScannedFile file, ClassIndex index) {
    Set<Path> dependencies = new LinkedHashSet<>();
    try {
      String source = Files.readString(file.absolutePath());
      for (String importedType : JavaSourceParser.parseImports(source)) {
        index
            .find(importedType)
            .ifPresent(target -> dependencies.add(target.relativePath()));
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read " + file.absolutePath(), e);
    }
    return dependencies;
  }

  private static String moduleForPath(Path relativePath, List<ScannedFile> files) {
    for (ScannedFile file : files) {
      if (file.relativePath().equals(relativePath)) {
        return file.module();
      }
    }
    throw new IllegalArgumentException("Unknown path: " + relativePath);
  }

  private static final class ModuleInfoBuilder {
    private int fileCount;
    private final Set<String> dependsOn = new LinkedHashSet<>();
    private final Set<String> usedBy = new LinkedHashSet<>();

    private ModuleInfoBuilder(String name) {}

    private void incrementFileCount() {
      fileCount++;
    }
  }
}
