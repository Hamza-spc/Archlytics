package com.archlytics.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AiResponseParser {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private AiResponseParser() {}

  public static AiAnalysisResult parse(String text) throws IOException {
    String jsonText = unwrapJson(text);
    JsonNode json = MAPPER.readTree(jsonText);

    List<AiAnalysisResult.Recommendation> recommendations = new ArrayList<>();
    for (JsonNode node : json.path("recommendations")) {
      recommendations.add(
          new AiAnalysisResult.Recommendation(
              node.path("title").asText(), node.path("detail").asText()));
    }

    List<AiAnalysisResult.SystemDesignIssue> systemDesignIssues = new ArrayList<>();
    for (JsonNode node : json.path("systemDesignIssues")) {
      systemDesignIssues.add(
          new AiAnalysisResult.SystemDesignIssue(
              node.path("title").asText(),
              node.path("impact").asText(),
              node.path("detail").asText()));
    }

    List<AiAnalysisResult.ScalingRisk> scalingRisks = new ArrayList<>();
    for (JsonNode node : json.path("scalingRisks")) {
      scalingRisks.add(
          new AiAnalysisResult.ScalingRisk(
              node.path("scenario").asText(),
              node.path("risk").asText(),
              node.path("mitigation").asText()));
    }

    return new AiAnalysisResult(
        json.path("architectureType").asText(),
        json.path("summary").asText(),
        recommendations,
        systemDesignIssues,
        scalingRisks,
        json.path("mermaidDiagram").asText());
  }

  static String unwrapJson(String text) {
    String trimmed = text.trim();
    if (trimmed.startsWith("```")) {
      int firstNewline = trimmed.indexOf('\n');
      int closing = trimmed.lastIndexOf("```");
      if (firstNewline >= 0 && closing > firstNewline) {
        return trimmed.substring(firstNewline + 1, closing).trim();
      }
    }
    return trimmed;
  }
}
