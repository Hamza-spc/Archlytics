package com.archlytics.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ReportFormatTest {

  @Test
  void fromString_parsesKnownFormats() {
    assertEquals(ReportFormat.MD, ReportFormat.fromString("md"));
    assertEquals(ReportFormat.HTML, ReportFormat.fromString("html"));
    assertEquals(ReportFormat.BOTH, ReportFormat.fromString("both"));
    assertEquals(ReportFormat.MD, ReportFormat.fromString("markdown"));
  }
}
