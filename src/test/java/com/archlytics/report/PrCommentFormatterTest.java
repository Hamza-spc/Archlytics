package com.archlytics.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.pr.PullRequestAnalysis;
import com.archlytics.rules.Severity;
import com.archlytics.rules.Violation;
import com.archlytics.snapshot.RunComparison;
import com.archlytics.snapshot.RunSnapshot;
import com.archlytics.snapshot.SnapshotComparer;
import com.archlytics.snapshot.SnapshotViolation;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrCommentFormatterTest {

  @Test
  void render_includesHealthScoreAndIntroducedViolations() {
    RunSnapshot baseline =
        new RunSnapshot(
            "2026-06-01 10:00:00",
            "/repo",
            78,
            "Needs attention",
            20,
            3,
            1,
            List.of("api", "core"),
            java.util.Map.of("api", List.of("core")),
            List.of());
    RunSnapshot current =
        new RunSnapshot(
            "2026-06-02 10:00:00",
            "/repo",
            72,
            "Needs attention",
            22,
            3,
            2,
            List.of("api", "core"),
            java.util.Map.of("api", List.of("core")),
            List.of(
                new SnapshotViolation(
                    "MEDIUM", "system-design", "Layer bypass detected", "api -> core")));
    RunComparison comparison = SnapshotComparer.compare(baseline, current);
    PullRequestAnalysis pr =
        new PullRequestAnalysis(
            "main",
            "HEAD",
            List.of(),
            java.util.Set.of("api"),
            List.of("api → core"),
            List.of(
                new Violation(
                    Severity.MEDIUM, "system-design", "Layer bypass", "api → core")),
            comparison);

    String comment = PrCommentFormatter.render(new HealthScore(72, "Needs attention", List.of()), pr);

    assertTrue(comment.contains("## Archlytics Report"));
    assertTrue(comment.contains("72/100"));
    assertTrue(comment.contains("Layer bypass"));
    assertTrue(comment.contains("api → core"));
  }
}
