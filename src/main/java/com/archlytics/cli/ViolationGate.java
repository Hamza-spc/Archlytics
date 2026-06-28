package com.archlytics.cli;

import com.archlytics.rules.Severity;
import com.archlytics.rules.Violation;
import java.util.List;

public final class ViolationGate {

  private ViolationGate() {}

  public static boolean shouldFail(List<Violation> violations, String failOn) {
    if (failOn == null || failOn.isBlank() || "none".equalsIgnoreCase(failOn)) {
      return false;
    }

    Severity threshold = parseThreshold(failOn);
    return violations.stream().anyMatch(v -> meetsThreshold(v.severity(), threshold));
  }

  static Severity parseThreshold(String failOn) {
    return switch (failOn.trim().toLowerCase()) {
      case "high" -> Severity.HIGH;
      case "medium" -> Severity.MEDIUM;
      case "low" -> Severity.LOW;
      default ->
          throw new IllegalArgumentException(
              "Invalid --fail-on value: "
                  + failOn
                  + " (expected none, low, medium, or high)");
    };
  }

  private static boolean meetsThreshold(Severity severity, Severity threshold) {
    return severity.ordinal() <= threshold.ordinal();
  }
}
