package com.archlytics.snapshot;

import com.archlytics.rules.Violation;

public record SnapshotViolation(String severity, String rule, String title, String evidence) {

  public static SnapshotViolation from(Violation violation) {
    return new SnapshotViolation(
        violation.severity().name(),
        violation.rule(),
        violation.title(),
        violation.evidence());
  }

  public String fingerprint() {
    return rule + "|" + title + "|" + evidence;
  }

  @Override
  public String toString() {
    return "[" + severity + "] " + title + " — " + evidence;
  }
}
