package com.archlytics.rules;

import com.archlytics.graph.DependencyGraph;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RuleEngine {

  private static final List<Rule> RULES =
      List.of(new CircularDependencyRule(), new HighCouplingRule());

  private RuleEngine() {}

  public static List<Violation> analyze(DependencyGraph graph) {
    List<Violation> violations = new ArrayList<>();
    for (Rule rule : RULES) {
      violations.addAll(rule.analyze(graph));
    }

    violations.sort(
        Comparator.comparing(Violation::severity)
            .thenComparing(Violation::rule)
            .thenComparing(Violation::title));

    return violations;
  }
}
