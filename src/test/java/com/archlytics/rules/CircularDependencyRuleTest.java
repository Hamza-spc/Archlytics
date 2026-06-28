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

class CircularDependencyRuleTest {

  @Test
  void analyze_detectsModuleCycle() {
    Map<String, Set<String>> edges = new LinkedHashMap<>();
    edges.put("a", Set.of("b"));
    edges.put("b", Set.of("c"));
    edges.put("c", Set.of("a"));

    DependencyGraph graph = graphWithModules("a", "b", "c", edges);
    List<Violation> violations = new CircularDependencyRule().analyze(graph);

    assertEquals(1, violations.size());
    assertTrue(violations.get(0).evidence().contains("a → b → c → a"));
  }

  @Test
  void normalizeCycle_rotatesToCanonicalForm() {
    List<String> cycle = List.of("b", "c", "a", "b");
    assertEquals("a→b→c→a", CircularDependencyRule.normalizeCycle(cycle));
  }

  private static DependencyGraph graphWithModules(
      String a, String b, String c, Map<String, Set<String>> edges) {
    Map<String, DependencyGraph.ModuleInfo> modules = new LinkedHashMap<>();
    modules.put(a, new DependencyGraph.ModuleInfo(1, Set.of("b"), Set.of("c")));
    modules.put(b, new DependencyGraph.ModuleInfo(1, Set.of("c"), Set.of("a")));
    modules.put(c, new DependencyGraph.ModuleInfo(1, Set.of("a"), Set.of("b")));
    return new DependencyGraph(Map.of(), edges, modules);
  }
}
