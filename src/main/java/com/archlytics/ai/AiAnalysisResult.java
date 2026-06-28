package com.archlytics.ai;

import java.util.List;

public record AiAnalysisResult(
    String architectureType,
    String summary,
    List<Recommendation> recommendations,
    List<SystemDesignIssue> systemDesignIssues,
    List<ScalingRisk> scalingRisks,
    String mermaidDiagram) {

  public record Recommendation(String title, String detail) {}

  public record SystemDesignIssue(String title, String impact, String detail) {}

  public record ScalingRisk(String scenario, String risk, String mitigation) {}
}
