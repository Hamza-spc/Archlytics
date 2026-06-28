package com.archlytics.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.graph.DependencyGraph;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HighCouplingRuleTest {

  @Test
  void analyze_flagsHighModuleFanOut() {
    Map<String, DependencyGraph.ModuleInfo> modules = new LinkedHashMap<>();
    modules.put(
        "hub",
        new DependencyGraph.ModuleInfo(
            1, Set.of("a", "b", "c"), Set.of()));

    DependencyGraph graph =
        new DependencyGraph(Map.of(), Map.of("hub", Set.of("a", "b", "c")), modules);

    List<Violation> violations = new HighCouplingRule().analyze(graph);

    assertEquals(1, violations.size());
    assertTrue(violations.get(0).title().contains("fan-out"));
  }

  @Test
  void analyze_flagsFileWithManyInternalDependencies() {
    Set<Path> deps = new LinkedHashSet<>();
    for (int i = 0; i < HighCouplingRule.FILE_DEPENDENCY_THRESHOLD; i++) {
      deps.add(Path.of("module/Dep" + i + ".java"));
    }

    DependencyGraph graph =
        new DependencyGraph(Map.of(Path.of("module/App.java"), deps), Map.of(), Map.of());

    List<Violation> violations = new HighCouplingRule().analyze(graph);

    assertEquals(1, violations.size());
    assertTrue(violations.get(0).evidence().contains("module/App.java"));
  }
}
