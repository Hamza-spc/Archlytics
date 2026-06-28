package com.archlytics.rules;

import com.archlytics.graph.DependencyGraph;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HighCouplingRule implements Rule {

  static final int MODULE_FAN_OUT_THRESHOLD = 3;
  static final int MODULE_FAN_IN_THRESHOLD = 3;
  static final int FILE_DEPENDENCY_THRESHOLD = 6;

  @Override
  public String name() {
    return "high-coupling";
  }

  @Override
  public List<Violation> analyze(DependencyGraph graph) {
    List<Violation> violations = new ArrayList<>();

    for (Map.Entry<String, DependencyGraph.ModuleInfo> entry : graph.modules().entrySet()) {
      String module = entry.getKey();
      DependencyGraph.ModuleInfo info = entry.getValue();

      if (info.dependsOn().size() >= MODULE_FAN_OUT_THRESHOLD) {
        violations.add(
            new Violation(
                Severity.MEDIUM,
                "high-coupling",
                "Module has high fan-out",
                module
                    + " depends on "
                    + info.dependsOn().size()
                    + " modules (threshold: "
                    + MODULE_FAN_OUT_THRESHOLD
                    + "): "
                    + String.join(", ", info.dependsOn())));
      }

      if (info.usedBy().size() >= MODULE_FAN_IN_THRESHOLD) {
        violations.add(
            new Violation(
                Severity.MEDIUM,
                "high-coupling",
                "Module has high fan-in",
                module
                    + " is used by "
                    + info.usedBy().size()
                    + " modules (threshold: "
                    + MODULE_FAN_IN_THRESHOLD
                    + "): "
                    + String.join(", ", info.usedBy())));
      }
    }

    for (Map.Entry<Path, Set<Path>> entry : graph.fileDependencies().entrySet()) {
      if (entry.getValue().size() >= FILE_DEPENDENCY_THRESHOLD) {
        violations.add(
            new Violation(
                Severity.LOW,
                "high-coupling",
                "File has many internal dependencies",
                entry.getKey()
                    + " imports "
                    + entry.getValue().size()
                    + " internal types (threshold: "
                    + FILE_DEPENDENCY_THRESHOLD
                    + ")"));
      }
    }

    return violations;
  }
}
