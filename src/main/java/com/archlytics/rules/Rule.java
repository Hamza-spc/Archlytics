package com.archlytics.rules;

import com.archlytics.config.ArchlyticsConfig;
import com.archlytics.graph.DependencyGraph;
import java.util.List;

public interface Rule {

  String name();

  List<Violation> analyze(DependencyGraph graph, ArchlyticsConfig config);
}
