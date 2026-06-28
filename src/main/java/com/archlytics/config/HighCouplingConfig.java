package com.archlytics.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HighCouplingConfig {

  public int fileImportThreshold = 6;
  public int moduleFanOutThreshold = 3;
  public int moduleFanInThreshold = 3;
}
