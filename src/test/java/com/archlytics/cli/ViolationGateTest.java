package com.archlytics.cli;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.rules.Severity;
import com.archlytics.rules.Violation;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViolationGateTest {

  @Test
  void shouldFail_respectsThreshold() {
    List<Violation> violations =
        List.of(
            new Violation(Severity.HIGH, "system-design", "Hub", "core"),
            new Violation(Severity.LOW, "high-coupling", "Coupling", "file"));

    assertFalse(ViolationGate.shouldFail(violations, "none"));
    assertFalse(ViolationGate.shouldFail(violations, null));
    assertTrue(ViolationGate.shouldFail(violations, "high"));
    assertTrue(ViolationGate.shouldFail(violations, "medium"));
    assertTrue(ViolationGate.shouldFail(violations, "low"));
    assertFalse(ViolationGate.shouldFail(List.of(violations.get(1)), "high"));
    assertTrue(ViolationGate.shouldFail(List.of(violations.get(1)), "low"));
  }

  @Test
  void parseThreshold_rejectsUnknownValue() {
    assertThrows(IllegalArgumentException.class, () -> ViolationGate.parseThreshold("critical"));
  }
}
