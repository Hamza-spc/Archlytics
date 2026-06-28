package com.archlytics.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SystemDesignConfig {

  public int serialChainThreshold = 3;
  public int hubFanInThreshold = 2;
  public int entryFanOutThreshold = 2;
}
