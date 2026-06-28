package com.archlytics.ai;

import com.archlytics.config.EnvLoader;
import java.util.Locale;

public enum AiProvider {
  GROQ,
  GEMINI;

  public static AiProvider resolve() {
    return EnvLoader.get("AI_PROVIDER")
        .map(value -> AiProvider.valueOf(value.trim().toUpperCase(Locale.ROOT)))
        .orElseGet(
            () -> {
              if (EnvLoader.get("GROQ_API_KEY").isPresent()) {
                return GROQ;
              }
              if (EnvLoader.get("GEMINI_API_KEY").isPresent()) {
                return GEMINI;
              }
              throw new IllegalStateException(
                  "No AI provider configured. Set GROQ_API_KEY or GEMINI_API_KEY in .env, "
                      + "optionally with AI_PROVIDER=groq|gemini.");
            });
  }
}
