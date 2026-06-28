package com.archlytics.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigLoaderTest {

  @TempDir Path tempDir;

  @Test
  void load_returnsDefaultsWhenNoConfigFile() {
    ArchlyticsConfig config = ConfigLoader.load(tempDir);

    assertEquals(6, config.rules.highCoupling.fileImportThreshold);
    assertEquals(3, config.rules.systemDesign.serialChainThreshold);
    assertTrue(config.ai.enabled);
  }

  @Test
  void load_readsYamlFromRepoRoot() throws Exception {
    Files.writeString(
        tempDir.resolve("archlytics.yaml"),
        """
        rules:
          highCoupling:
            fileImportThreshold: 10
          systemDesign:
            hubFanInThreshold: 3
        ignore:
          modules:
            - legacy-module
        ai:
          enabled: false
          provider: groq
        """);

    ArchlyticsConfig config = ConfigLoader.load(tempDir);

    assertEquals(10, config.rules.highCoupling.fileImportThreshold);
    assertEquals(3, config.rules.systemDesign.hubFanInThreshold);
    assertEquals(1, config.ignore.modules.size());
    assertEquals("legacy-module", config.ignore.modules.get(0));
    assertEquals(false, config.ai.enabled);
    assertEquals("groq", config.ai.provider);
  }

  @Test
  void load_explicitConfigPath() throws Exception {
    Path custom = tempDir.resolve("custom-config.yaml");
    Files.writeString(
        custom,
        """
        rules:
          highCoupling:
            moduleFanOutThreshold: 5
        """);

    ArchlyticsConfig config = ConfigLoader.load(tempDir, custom);

    assertEquals(5, config.rules.highCoupling.moduleFanOutThreshold);
    assertEquals(3, config.rules.highCoupling.moduleFanInThreshold);
  }
}
