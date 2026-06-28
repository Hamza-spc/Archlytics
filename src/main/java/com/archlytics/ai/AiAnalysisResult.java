package com.archlytics.ai;

import java.util.List;

public record AiAnalysisResult(
    String architectureType, String summary, List<Recommendation> recommendations, String mermaidDiagram) {

  public record Recommendation(String title, String detail) {}
}
