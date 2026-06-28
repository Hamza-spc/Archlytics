package com.archlytics.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GeminiClient {

  private static final String DEFAULT_MODEL = "gemini-2.5-flash";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String apiKey;
  private final String model;
  private final HttpClient httpClient;

  public GeminiClient(String apiKey) {
    this(apiKey, DEFAULT_MODEL);
  }

  public GeminiClient(String apiKey, String model) {
    this.apiKey = apiKey;
    this.model = model != null && !model.isBlank() ? model : DEFAULT_MODEL;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
  }

  public AiAnalysisResult analyze(String prompt) {
    try {
      String requestBody = buildRequestBody(prompt);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(
                  URI.create(
                      "https://generativelanguage.googleapis.com/v1beta/models/"
                          + model
                          + ":generateContent?key="
                          + apiKey))
              .header("Content-Type", "application/json")
              .timeout(Duration.ofSeconds(60))
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new IllegalStateException(
            "Gemini API error (HTTP "
                + response.statusCode()
                + "): "
                + truncate(response.body(), 500));
      }

      return parseResponse(response.body());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Gemini request interrupted", e);
    } catch (IOException e) {
      throw new IllegalStateException("Gemini request failed", e);
    }
  }

  private static String buildRequestBody(String prompt) throws IOException {
    Map<String, Object> body =
        Map.of(
            "contents",
            List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig",
            Map.of("responseMimeType", "application/json"));
    return MAPPER.writeValueAsString(body);
  }

  private static AiAnalysisResult parseResponse(String responseBody) throws IOException {
    JsonNode root = MAPPER.readTree(responseBody);
    String text =
        root.path("candidates")
            .path(0)
            .path("content")
            .path("parts")
            .path(0)
            .path("text")
            .asText();

    if (text == null || text.isBlank()) {
      throw new IllegalStateException("Empty response from Gemini: " + truncate(responseBody, 300));
    }

    JsonNode json = MAPPER.readTree(text);
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

  private static String truncate(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max) + "...";
  }
}
