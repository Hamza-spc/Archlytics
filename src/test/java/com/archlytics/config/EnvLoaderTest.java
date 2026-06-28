package com.archlytics.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnvLoaderTest {

  @TempDir Path tempDir;

  @Test
  void loadFile_parsesKeyValuePairs() throws Exception {
    Path envFile = tempDir.resolve(".env");
    Files.writeString(
        envFile,
        """
        # comment
        GEMINI_API_KEY=test-key-123
        OTHER=value
        """);

    HashMap<String, String> env = new HashMap<>();
    EnvLoader.loadFile(envFile, env);

    assertEquals("test-key-123", env.get("GEMINI_API_KEY"));
    assertEquals("value", env.get("OTHER"));
  }

  @Test
  void findEnvFile_walksUpDirectoryTree() throws Exception {
    Path nested = tempDir.resolve("a/b/c");
    Files.createDirectories(nested);
    Files.writeString(tempDir.resolve(".env"), "GEMINI_API_KEY=root\n");

    assertEquals(tempDir.resolve(".env"), EnvLoader.findEnvFile(nested).orElseThrow());
  }
}
