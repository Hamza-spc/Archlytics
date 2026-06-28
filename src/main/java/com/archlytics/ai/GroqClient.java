package com.archlytics.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public final class GroqClient implements AiClient {

  private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
  private static final String DEFAULT_MODEL = "llama-3.3-70b-versatile";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final String apiKey;
  private final String model;
  private final HttpClient httpClient;

  public GroqClient(String apiKey, String model) {
    this.apiKey = apiKey;
    this.model = model != null && !model.isBlank() ? model : DEFAULT_MODEL;
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
  }

  @Override
  public AiAnalysisResult analyze(String prompt) {
    try {
      String requestBody = buildRequestBody(prompt);
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(ENDPOINT))
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + apiKey)
              .timeout(Duration.ofSeconds(60))
              .POST(HttpRequest.BodyPublishers.ofString(requestBody))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new IllegalStateException(
            "Groq API error (HTTP "
                + response.statusCode()
                + "): "
                + truncate(response.body(), 500));
      }

      return parseResponse(response.body());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Groq request interrupted", e);
    } catch (IOException e) {
      throw new IllegalStateException("Groq request failed", e);
    }
  }

  private String buildRequestBody(String prompt) throws IOException {
    Map<String, Object> body =
        Map.of(
            "model",
            model,
            "messages",
            List.of(
                Map.of(
                    "role",
                    "system",
                    "content",
                    "You are a software architect. Respond with valid JSON only, no markdown."),
                Map.of("role", "user", "content", prompt)),
            "response_format",
            Map.of("type", "json_object"),
            "temperature",
            0.2);
    return MAPPER.writeValueAsString(body);
  }

  private static AiAnalysisResult parseResponse(String responseBody) throws IOException {
    JsonNode root = MAPPER.readTree(responseBody);
    String text = root.path("choices").path(0).path("message").path("content").asText();

    if (text == null || text.isBlank()) {
      throw new IllegalStateException("Empty response from Groq: " + truncate(responseBody, 300));
    }

    return AiResponseParser.parse(text);
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max) + "...";
  }
}
