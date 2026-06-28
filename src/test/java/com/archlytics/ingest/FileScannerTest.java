package com.archlytics.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.config.ArchlyticsConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileScannerTest {

  @TempDir Path tempDir;

  @Test
  void scan_findsJavaFiles_andSkipsIgnoredDirectories() throws Exception {
    Path src =
        tempDir.resolve("module-a/src/main/java/com/example/App.java");
    Files.createDirectories(src.getParent());
    Files.writeString(src, "package com.example;");

    Path ignored =
        tempDir.resolve("module-a/target/generated/Generated.java");
    Files.createDirectories(ignored.getParent());
    Files.writeString(ignored, "class Generated {}");

    Path nodeModules =
        tempDir.resolve("node_modules/lib/Lib.java");
    Files.createDirectories(nodeModules.getParent());
    Files.writeString(nodeModules, "class Lib {}");

    List<ScannedFile> files = FileScanner.scan(tempDir);

    assertEquals(1, files.size());
    assertTrue(files.get(0).relativePath().toString().endsWith("App.java"));
    assertEquals("module-a", files.get(0).module());
  }

  @Test
  void inferModule_usesMavenModuleDirectory() {
    Path relative = Path.of("java/maroctax-api/src/main/java/ma/maroctax/api/App.java");
    assertEquals("maroctax-api", FileScanner.inferModule(relative));
  }

  @Test
  void scan_skipsIgnoredModules() throws Exception {
    Path kept =
        tempDir.resolve("keep-module/src/main/java/com/example/Keep.java");
    Files.createDirectories(kept.getParent());
    Files.writeString(kept, "package com.example;");

    Path ignored =
        tempDir.resolve("legacy-module/src/main/java/com/example/Legacy.java");
    Files.createDirectories(ignored.getParent());
    Files.writeString(ignored, "package com.example;");

    ArchlyticsConfig config = ArchlyticsConfig.defaults();
    config.ignore.modules = List.of("legacy-module");

    List<ScannedFile> files = FileScanner.scan(tempDir, config);

    assertEquals(1, files.size());
    assertEquals("keep-module", files.get(0).module());
  }
}
