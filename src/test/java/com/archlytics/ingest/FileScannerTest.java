package com.archlytics.ingest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    assertEquals("com", files.get(0).module());
  }

  @Test
  void inferModule_usesPackageRootAfterSrcMainJava() {
    Path relative = Path.of("java/maroctax-api/src/main/java/ma/maroctax/api/App.java");
    assertEquals("ma", FileScanner.inferModule(relative));
  }
}
