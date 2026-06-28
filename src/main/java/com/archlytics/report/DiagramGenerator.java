package com.archlytics.report;

import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphMetrics;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DiagramGenerator {

  private DiagramGenerator() {}

  public static ArchitectureDiagrams generate(DependencyGraph graph) {
    return new ArchitectureDiagrams(
        moduleDependencyDiagram(graph),
        layerViewDiagram(graph),
        criticalPathDiagram(graph));
  }

  public static String moduleDependencyDiagram(DependencyGraph graph) {
    StringBuilder mermaid = new StringBuilder("graph TD\n");
    Set<String> nodes = graph.modules().keySet();

    for (String module : nodes) {
      mermaid.append("  ").append(nodeId(module)).append("[").append(module).append("]\n");
    }

    for (Map.Entry<String, Set<String>> entry : graph.moduleDependencies().entrySet()) {
      for (String target : entry.getValue()) {
        mermaid
            .append("  ")
            .append(nodeId(entry.getKey()))
            .append(" --> ")
            .append(nodeId(target))
            .append("\n");
      }
    }

    return mermaid.toString().trim();
  }

  public static String layerViewDiagram(DependencyGraph graph) {
    Map<String, String> layers = classifyLayers(graph);
    Map<String, List<String>> grouped = new LinkedHashMap<>();
    grouped.put("Presentation Layer", new ArrayList<>());
    grouped.put("Integration Layer", new ArrayList<>());
    grouped.put("Domain Layer", new ArrayList<>());

    for (String module : graph.modules().keySet()) {
      grouped.get(layers.get(module)).add(module);
    }

    StringBuilder mermaid = new StringBuilder("graph TB\n");
    int subgraphIndex = 0;
    for (Map.Entry<String, List<String>> layer : grouped.entrySet()) {
      if (layer.getValue().isEmpty()) {
        continue;
      }
      mermaid.append("  subgraph L").append(subgraphIndex++).append(" [").append(layer.getKey()).append("]\n");
      for (String module : layer.getValue()) {
        mermaid.append("    ").append(nodeId(module)).append("[").append(module).append("]\n");
      }
      mermaid.append("  end\n");
    }

    for (Map.Entry<String, Set<String>> entry : graph.moduleDependencies().entrySet()) {
      for (String target : entry.getValue()) {
        mermaid
            .append("  ")
            .append(nodeId(entry.getKey()))
            .append(" --> ")
            .append(nodeId(target))
            .append("\n");
      }
    }

    return mermaid.toString().trim();
  }

  public static String criticalPathDiagram(DependencyGraph graph) {
    List<String> chain = GraphMetrics.longestDependencyChain(graph.modules().keySet(), graph.moduleDependencies());
    if (chain.size() < 2) {
      return moduleDependencyDiagram(graph);
    }

    StringBuilder mermaid = new StringBuilder("graph LR\n");
    for (String module : chain) {
      mermaid.append("  ").append(nodeId(module)).append("[").append(module).append("]\n");
    }

    for (int i = 0; i < chain.size() - 1; i++) {
      mermaid
          .append("  ")
          .append(nodeId(chain.get(i)))
          .append(" --> ")
          .append(nodeId(chain.get(i + 1)))
          .append("\n");
    }

    return mermaid.toString().trim();
  }

  static Map<String, String> classifyLayers(DependencyGraph graph) {
    Map<String, String> layers = new LinkedHashMap<>();
    for (Map.Entry<String, DependencyGraph.ModuleInfo> entry : graph.modules().entrySet()) {
      DependencyGraph.ModuleInfo info = entry.getValue();
      if (info.usedBy().isEmpty()) {
        layers.put(entry.getKey(), "Presentation Layer");
      } else if (info.dependsOn().isEmpty()) {
        layers.put(entry.getKey(), "Domain Layer");
      } else {
        layers.put(entry.getKey(), "Integration Layer");
      }
    }
    return layers;
  }

  static String nodeId(String module) {
    String id = module.replaceAll("[^a-zA-Z0-9]", "_");
    if (id.isEmpty() || Character.isDigit(id.charAt(0))) {
      return "m_" + id;
    }
    return id;
  }
}
