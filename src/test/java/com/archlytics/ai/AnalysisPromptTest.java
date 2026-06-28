package com.archlytics.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.graph.DependencyGraph;
import com.archlytics.rules.Violation;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AnalysisPromptTest {

  @Test
  void build_includesModulesAndViolations() {
    DependencyGraph graph =
        new DependencyGraph(
            Map.of(),
            Map.of("maroctax-api", Set.of("maroctax-core")),
            Map.of(
                "maroctax-api",
                new DependencyGraph.ModuleInfo(
                    9, Set.of("maroctax-core"), Set.of()),
                "maroctax-core",
                new DependencyGraph.ModuleInfo(14, Set.of(), Set.of("maroctax-api"))));

    String prompt =
        AnalysisPrompt.build(
            "/repo",
            graph,
            List.of(
                new Violation(
                    com.archlytics.rules.Severity.LOW,
                    "high-coupling",
                    "File has many internal dependencies",
                    "Controller.java imports 8 types")),
            28);

    assertTrue(prompt.contains("maroctax-api"));
    assertTrue(prompt.contains("maroctax-core"));
    assertTrue(prompt.contains("Controller.java imports 8 types"));
    assertTrue(prompt.contains("architectureType"));
  }
}
