package com.archlytics.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RulesConfig {

  public HighCouplingConfig highCoupling = new HighCouplingConfig();
  public SystemDesignConfig systemDesign = new SystemDesignConfig();
}
