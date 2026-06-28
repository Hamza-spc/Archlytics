package com.archlytics.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.graph.DependencyGraph;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SystemDesignRuleTest {

  @Test
  void analyze_flagsSharedKernelAndLayerBypass() {
    DependencyGraph graph =
        new DependencyGraph(
            Map.of(),
            Map.of("maroctax-api", Set.of("maroctax-core", "maroctax-spring-boot-starter"),
                "maroctax-spring-boot-starter", Set.of("maroctax-core")),
            Map.of(
                "maroctax-api",
                    new DependencyGraph.ModuleInfo(
                        9, Set.of("maroctax-core", "maroctax-spring-boot-starter"), Set.of()),
                "maroctax-spring-boot-starter",
                    new DependencyGraph.ModuleInfo(5, Set.of("maroctax-core"), Set.of("maroctax-api")),
                "maroctax-core",
                    new DependencyGraph.ModuleInfo(
                        14, Set.of(), Set.of("maroctax-api", "maroctax-spring-boot-starter"))));

    List<Violation> violations = new SystemDesignRule().analyze(graph);

    assertTrue(
        violations.stream().anyMatch(v -> v.title().contains("Shared kernel bottleneck")));
    assertTrue(violations.stream().anyMatch(v -> v.title().contains("Layer bypass")));
    assertTrue(
        violations.stream().anyMatch(v -> v.title().contains("Entry module fans out")));
  }
}
