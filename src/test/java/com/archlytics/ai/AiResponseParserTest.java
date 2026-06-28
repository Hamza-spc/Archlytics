package com.archlytics.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AiResponseParserTest {

  private static final String SAMPLE =
      """
      {
        "architectureType": "Layered monolith",
        "summary": "Three modules with clear layering.",
        "recommendations": [{"title": "Split core", "detail": "Extract domain services"}],
        "systemDesignIssues": [{"title": "Hub module", "impact": "HIGH", "detail": "Core is shared"}],
        "scalingRisks": [{"scenario": "10x traffic", "risk": "Core saturation", "mitigation": "Cache"}],
        "mermaidDiagram": "graph TD\\n  api-->core"
      }
      """;

  @Test
  void parse_readsStructuredResponse() throws Exception {
    AiAnalysisResult result = AiResponseParser.parse(SAMPLE);

    assertEquals("Layered monolith", result.architectureType());
    assertEquals(1, result.recommendations().size());
    assertEquals(1, result.systemDesignIssues().size());
    assertEquals(1, result.scalingRisks().size());
    assertTrue(result.mermaidDiagram().contains("api-->core"));
  }

  @Test
  void unwrapJson_stripsMarkdownFence() {
    String wrapped = "```json\n" + SAMPLE + "\n```";
    assertTrue(AiResponseParser.unwrapJson(wrapped).startsWith("{"));
  }
}
