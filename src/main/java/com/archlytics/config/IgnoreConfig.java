package com.archlytics.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IgnoreConfig {

  public List<String> modules = new ArrayList<>();
  public List<String> directories = new ArrayList<>();
  public List<String> pathPatterns = new ArrayList<>();
}
