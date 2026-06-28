package com.archlytics.report;

public enum ReportFormat {
  MD,
  HTML,
  BOTH;

  public static ReportFormat fromString(String value) {
    if (value == null || value.isBlank()) {
      return MD;
    }
    return switch (value.trim().toLowerCase()) {
      case "md", "markdown" -> MD;
      case "html" -> HTML;
      case "both" -> BOTH;
      default -> throw new IllegalArgumentException("Unknown format: " + value + " (use md, html, or both)");
    };
  }
}
