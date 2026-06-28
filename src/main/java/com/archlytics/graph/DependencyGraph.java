package com.archlytics.graph;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public record DependencyGraph(
    Map<Path, Set<Path>> fileDependencies,
    Map<String, Set<String>> moduleDependencies,
    Map<String, ModuleInfo> modules) {

  public record ModuleInfo(int fileCount, Set<String> dependsOn, Set<String> usedBy) {}
}
