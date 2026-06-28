package com.archlytics.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GraphMetrics {

  private GraphMetrics() {}

  public static Metrics compute(DependencyGraph graph) {
    Map<String, Set<String>> edges = graph.moduleDependencies();
    Set<String> allModules = graph.modules().keySet();

    List<String> longestChain = longestDependencyChain(allModules, edges);
    List<String> hubs = findHubModules(graph);
    List<String> entryPoints = findEntryPoints(graph);

    return new Metrics(longestChain, hubs, entryPoints);
  }

  public static List<String> longestDependencyChain(
      Set<String> modules, Map<String, Set<String>> edges) {
    List<String> best = new ArrayList<>();
    for (String start : modules) {
      exploreChain(start, edges, new ArrayList<>(), new HashSet<>(), best);
    }
    return best;
  }

  public static List<String> findHubModules(DependencyGraph graph) {
    return graph.modules().entrySet().stream()
        .filter(e -> e.getValue().usedBy().size() >= 2)
        .sorted(Comparator.comparingInt((Map.Entry<String, DependencyGraph.ModuleInfo> e) -> -e.getValue().usedBy().size())
            .thenComparing(Map.Entry::getKey))
        .map(Map.Entry::getKey)
        .toList();
  }

  public static List<String> findEntryPoints(DependencyGraph graph) {
    return graph.modules().entrySet().stream()
        .filter(e -> e.getValue().usedBy().isEmpty())
        .map(Map.Entry::getKey)
        .sorted()
        .toList();
  }

  public static List<String> findLayerBypasses(DependencyGraph graph) {
    List<String> bypasses = new ArrayList<>();
    for (Map.Entry<String, Set<String>> entry : graph.moduleDependencies().entrySet()) {
      String source = entry.getKey();
      for (String direct : entry.getValue()) {
        for (String indirect : entry.getValue()) {
          if (direct.equals(indirect)) {
            continue;
          }
          if (graph.moduleDependencies().getOrDefault(indirect, Set.of()).contains(direct)) {
            bypasses.add(
                source + " depends directly on " + direct + " while also using " + indirect);
          }
        }
      }
    }
    return bypasses;
  }

  public static String toSummary(Metrics metrics, DependencyGraph graph) {
    StringBuilder sb = new StringBuilder();
    sb.append("Longest dependency chain: ")
        .append(metrics.longestChain().isEmpty() ? "none" : String.join(" → ", metrics.longestChain()))
        .append('\n');
    sb.append("Entry-point modules: ")
        .append(metrics.entryPoints().isEmpty() ? "none" : String.join(", ", metrics.entryPoints()))
        .append('\n');
    sb.append("Shared-kernel hubs (fan-in ≥ 2): ")
        .append(metrics.hubModules().isEmpty() ? "none" : String.join(", ", metrics.hubModules()))
        .append('\n');

    List<String> bypasses = findLayerBypasses(graph);
    sb.append("Layer bypasses: ")
        .append(bypasses.isEmpty() ? "none" : String.join("; ", bypasses))
        .append('\n');

    return sb.toString();
  }

  private static void exploreChain(
      String current,
      Map<String, Set<String>> edges,
      List<String> path,
      Set<String> visiting,
      List<String> best) {
    if (visiting.contains(current)) {
      return;
    }

    path.add(current);
    visiting.add(current);

    if (path.size() > best.size()) {
      best.clear();
      best.addAll(path);
    }

    for (String next : edges.getOrDefault(current, Set.of())) {
      exploreChain(next, edges, path, visiting, best);
    }

    path.remove(path.size() - 1);
    visiting.remove(current);
  }

  public record Metrics(List<String> longestChain, List<String> hubModules, List<String> entryPoints) {}

}
