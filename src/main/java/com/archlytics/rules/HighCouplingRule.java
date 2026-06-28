package com.archlytics.rules;

import com.archlytics.config.ArchlyticsConfig;
import com.archlytics.graph.DependencyGraph;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HighCouplingRule implements Rule {

  @Override
  public String name() {
    return "high-coupling";
  }

  @Override
  public List<Violation> analyze(DependencyGraph graph, ArchlyticsConfig config) {
    int fanOutThreshold = config.rules.highCoupling.moduleFanOutThreshold;
    int fanInThreshold = config.rules.highCoupling.moduleFanInThreshold;
    int fileThreshold = config.rules.highCoupling.fileImportThreshold;

    List<Violation> violations = new ArrayList<>();

    for (Map.Entry<String, DependencyGraph.ModuleInfo> entry : graph.modules().entrySet()) {
      String module = entry.getKey();
      DependencyGraph.ModuleInfo info = entry.getValue();

      if (info.dependsOn().size() >= fanOutThreshold) {
        violations.add(
            new Violation(
                Severity.MEDIUM,
                "high-coupling",
                "Module has high fan-out",
                module
                    + " depends on "
                    + info.dependsOn().size()
                    + " modules (threshold: "
                    + fanOutThreshold
                    + "): "
                    + String.join(", ", info.dependsOn())));
      }

      if (info.usedBy().size() >= fanInThreshold) {
        violations.add(
            new Violation(
                Severity.MEDIUM,
                "high-coupling",
                "Module has high fan-in",
                module
                    + " is used by "
                    + info.usedBy().size()
                    + " modules (threshold: "
                    + fanInThreshold
                    + "): "
                    + String.join(", ", info.usedBy())));
      }
    }

    for (Map.Entry<Path, Set<Path>> entry : graph.fileDependencies().entrySet()) {
      if (entry.getValue().size() >= fileThreshold) {
        violations.add(
            new Violation(
                Severity.LOW,
                "high-coupling",
                "File has many internal dependencies",
                entry.getKey()
                    + " imports "
                    + entry.getValue().size()
                    + " internal types (threshold: "
                    + fileThreshold
                    + ")"));
      }
    }

    return violations;
  }
}
