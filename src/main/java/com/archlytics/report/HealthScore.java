package com.archlytics.report;

import java.util.List;

public record HealthScore(int score, String label, List<String> topRisks) {}
