package com.archlytics.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class JavaSourceParserTest {

  private static final String SOURCE =
      """
      package ma.maroctax.api.controller;

      import java.math.BigDecimal;
      import ma.maroctax.FiscalYear;
      import ma.maroctax.spring.MarocTaxService;
      import static org.junit.jupiter.api.Assertions.assertEquals;

      public class PayrollController {}
      """;

  @Test
  void parsePackage_extractsPackageName() {
    assertEquals("ma.maroctax.api.controller", JavaSourceParser.parsePackage(SOURCE).orElseThrow());
  }

  @Test
  void parseImports_extractsNonWildcardImports() {
    var imports = JavaSourceParser.parseImports(SOURCE);
    assertEquals(3, imports.size());
    assertTrue(imports.contains("java.math.BigDecimal"));
    assertTrue(imports.contains("ma.maroctax.FiscalYear"));
    assertTrue(imports.contains("ma.maroctax.spring.MarocTaxService"));
  }
}
