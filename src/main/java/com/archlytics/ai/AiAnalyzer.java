package com.archlytics.ai;

import com.archlytics.config.EnvLoader;
import com.archlytics.graph.DependencyGraph;
import com.archlytics.graph.GraphMetrics;
import com.archlytics.rules.Violation;
import java.util.List;

public final class AiAnalyzer {

  private AiAnalyzer() {}

  public static AiAnalysisResult analyze(
      String repoPath,
      DependencyGraph graph,
      GraphMetrics.Metrics metrics,
      List<Violation> violations,
      int fileCount) {
    AiProvider provider = AiProvider.resolve();
    String prompt = AnalysisPrompt.build(repoPath, graph, metrics, violations, fileCount);
    return createClient(provider).analyze(prompt);
  }

  public static AiProvider configuredProvider() {
    return AiProvider.resolve();
  }

  private static AiClient createClient(AiProvider provider) {
    return switch (provider) {
      case GROQ -> {
        String apiKey =
            EnvLoader.get("GROQ_API_KEY")
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "GROQ_API_KEY not found. Get a free key at https://console.groq.com"));
        String model = EnvLoader.get("GROQ_MODEL").orElse(null);
        yield new GroqClient(apiKey, model);
      }
      case GEMINI -> {
        String apiKey =
            EnvLoader.get("GEMINI_API_KEY")
                .orElseThrow(
                    () ->
                        new IllegalStateException(
                            "GEMINI_API_KEY not found. Set it in .env or as an environment variable."));
        String model = EnvLoader.get("GEMINI_MODEL").orElse(null);
        yield new GeminiClient(apiKey, model);
      }
    };
  }
}
