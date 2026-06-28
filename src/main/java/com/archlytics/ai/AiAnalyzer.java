package com.archlytics.ai;

import com.archlytics.config.EnvLoader;
import com.archlytics.graph.DependencyGraph;
import com.archlytics.rules.Violation;
import java.util.List;

public final class AiAnalyzer {

  private AiAnalyzer() {}

  public static AiAnalysisResult analyze(
      String repoPath, DependencyGraph graph, List<Violation> violations, int fileCount) {
    String apiKey =
        EnvLoader.get("GEMINI_API_KEY")
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "GEMINI_API_KEY not found. Set it in .env or as an environment variable."));

    String model = EnvLoader.get("GEMINI_MODEL").orElse(null);
    String prompt = AnalysisPrompt.build(repoPath, graph, violations, fileCount);
    return new GeminiClient(apiKey, model).analyze(prompt);
  }
}
