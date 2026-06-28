package com.archlytics.rules;

public record Violation(Severity severity, String rule, String title, String evidence) {}
