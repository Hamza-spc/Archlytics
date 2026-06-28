package com.archlytics.rules;

import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SystemDesignRule implements Rule {

  static final int SERIAL_CHAIN_THRESHOLD = 3;

  @Override
  public String name() {
    return "system-design";
  }

  @Override
  public List<Violation> analyze(DependencyGraph graph) {
    List<Violation> violations = new ArrayList<>();
    GraphMetrics.Metrics metrics = GraphMetrics.compute(graph);

    for (String hub : metrics.hubModules()) {
      int fanIn = graph.modules().get(hub).usedBy().size();
      violations.add(
          new Violation(
              Severity.HIGH,
              "system-design",
              "Shared kernel bottleneck",
              hub
                  + " is depended on by "
                  + fanIn
                  + " modules — under load, this becomes a central scaling bottleneck and single point of contention"));
    }

    if (metrics.longestChain().size() >= SERIAL_CHAIN_THRESHOLD) {
      violations.add(
          new Violation(
              Severity.MEDIUM,
              "system-design",
              "Serial dependency chain",
              "Longest synchronous module path is "
                  + metrics.longestChain().size()
                  + " modules: "
                  + String.join(" → ", metrics.longestChain())
                  + " — deep chains increase latency and prevent independent scaling"));
    }

    for (String bypass : GraphMetrics.findLayerBypasses(graph)) {
      violations.add(
          new Violation(
              Severity.MEDIUM,
              "system-design",
              "Layer bypass detected",
              bypass + " — bypassing an intermediate module couples layers and complicates scaling"));
    }

    for (Map.Entry<String, DependencyGraph.ModuleInfo> entry : graph.modules().entrySet()) {
      if (entry.getValue().dependsOn().size() >= 2 && entry.getValue().usedBy().isEmpty()) {
        violations.add(
            new Violation(
                Severity.LOW,
                "system-design",
                "Entry module fans out to multiple backends",
                entry.getKey()
                    + " depends directly on "
                    + entry.getValue().dependsOn().size()
                    + " modules ("
                    + String.join(", ", entry.getValue().dependsOn())
                    + ") — request handling may fan out synchronously under traffic"));
      }
    }

    return violations;
  }
}
