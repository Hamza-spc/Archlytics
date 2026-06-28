package com.archlytics.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.graph.DependencyGraph;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DiagramGeneratorTest {

  @Test
  void generate_producesThreeDiagramViews() {
    DependencyGraph graph =
        new DependencyGraph(
            Map.of(),
            Map.of("api", Set.of("core", "starter"), "starter", Set.of("core")),
            Map.of(
                "api", new DependencyGraph.ModuleInfo(3, Set.of("core", "starter"), Set.of()),
                "starter", new DependencyGraph.ModuleInfo(2, Set.of("core"), Set.of("api")),
                "core", new DependencyGraph.ModuleInfo(5, Set.of(), Set.of("api", "starter"))));

    ArchitectureDiagrams diagrams = DiagramGenerator.generate(graph);

    assertTrue(diagrams.moduleDependency().contains("api --> core"));
    assertTrue(diagrams.layerView().contains("Presentation Layer"));
    assertTrue(diagrams.criticalPath().contains("api"));
    assertTrue(diagrams.criticalPath().contains("core"));
  }
}
