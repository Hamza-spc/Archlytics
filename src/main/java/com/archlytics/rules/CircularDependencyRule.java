package com.archlytics.rules;

import com.archlytics.graph.DependencyGraph;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CircularDependencyRule implements Rule {

  @Override
  public String name() {
    return "circular-dependency";
  }

  @Override
  public List<Violation> analyze(DependencyGraph graph) {
    List<Violation> violations = new ArrayList<>();
    Set<String> reported = new HashSet<>();

    for (String module : graph.modules().keySet()) {
      findCycles(module, graph.moduleDependencies(), new ArrayList<>(), reported, violations);
    }

    return violations;
  }

  private static void findCycles(
      String current,
      Map<String, Set<String>> edges,
      List<String> path,
      Set<String> reported,
      List<Violation> violations) {
    int cycleStart = path.indexOf(current);
    if (cycleStart >= 0) {
      List<String> cycle = new ArrayList<>(path.subList(cycleStart, path.size()));
      cycle.add(current);
      String key = normalizeCycle(cycle);
      if (reported.add(key)) {
        violations.add(
            new Violation(
                Severity.HIGH,
                "circular-dependency",
                "Circular module dependency detected",
                String.join(" → ", cycle)));
      }
      return;
    }

    path.add(current);
    for (String next : edges.getOrDefault(current, Set.of())) {
      findCycles(next, edges, path, reported, violations);
    }
    path.remove(path.size() - 1);
  }

  static String normalizeCycle(List<String> cycle) {
    if (cycle.size() < 2) {
      return String.join("→", cycle);
    }

    List<String> nodes = cycle.subList(0, cycle.size() - 1);
    int start = 0;
    for (int i = 1; i < nodes.size(); i++) {
      if (nodes.get(i).compareTo(nodes.get(start)) < 0) {
        start = i;
      }
    }

    List<String> rotated = new ArrayList<>();
    rotated.addAll(nodes.subList(start, nodes.size()));
    rotated.addAll(nodes.subList(0, start));
    rotated.add(rotated.get(0));
    return String.join("→", rotated);
  }
}
