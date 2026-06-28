package com.archlytics.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GraphMetricsTest {

  @Test
  void longestDependencyChain_findsLongestPath() {
    Map<String, Set<String>> edges =
        Map.of(
            "maroctax-api", Set.of("maroctax-spring-boot-starter", "maroctax-core"),
            "maroctax-spring-boot-starter", Set.of("maroctax-core"));

    Set<String> modules = Set.of("maroctax-api", "maroctax-spring-boot-starter", "maroctax-core");
    var chain = GraphMetrics.longestDependencyChain(modules, edges);

    assertEquals(3, chain.size());
    assertEquals("maroctax-api", chain.get(0));
    assertEquals("maroctax-core", chain.get(chain.size() - 1));
  }

  @Test
  void findHubModules_detectsSharedKernel() {
    DependencyGraph graph =
        new DependencyGraph(
            Map.of(),
            Map.of(
                "api", Set.of("core", "starter"),
                "starter", Set.of("core")),
            Map.of(
                "api", new DependencyGraph.ModuleInfo(1, Set.of("core", "starter"), Set.of()),
                "starter",
                    new DependencyGraph.ModuleInfo(1, Set.of("core"), Set.of("api")),
                "core", new DependencyGraph.ModuleInfo(5, Set.of(), Set.of("api", "starter"))));

    assertEquals(List.of("core"), GraphMetrics.findHubModules(graph));
  }

  @Test
  void findLayerBypasses_detectsDirectAndWrappedDependency() {
    DependencyGraph graph =
        new DependencyGraph(
            Map.of(),
            Map.of("api", Set.of("core", "starter"), "starter", Set.of("core")),
            Map.of(
                "api", new DependencyGraph.ModuleInfo(1, Set.of("core", "starter"), Set.of()),
                "starter", new DependencyGraph.ModuleInfo(1, Set.of("core"), Set.of("api")),
                "core", new DependencyGraph.ModuleInfo(1, Set.of(), Set.of("api", "starter"))));

    assertTrue(
        GraphMetrics.findLayerBypasses(graph).stream()
            .anyMatch(b -> b.contains("api") && b.contains("core")));
  }
}
