package com.archlytics.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArchlyticsConfig {

  public RulesConfig rules = new RulesConfig();
  public IgnoreConfig ignore = new IgnoreConfig();
  public AiConfig ai = new AiConfig();

  public static ArchlyticsConfig defaults() {
    return new ArchlyticsConfig();
  }
}
