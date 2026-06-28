package com.archlytics.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AiConfig {

  public boolean enabled = true;
  public String provider;
}
