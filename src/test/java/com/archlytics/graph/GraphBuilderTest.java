package com.archlytics.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.ingest.FileScanner;
import com.archlytics.ingest.ScannedFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GraphBuilderTest {

  @TempDir Path tempDir;

  @Test
  void build_resolvesInternalImportsAcrossModules() throws Exception {
    Path coreFile =
        tempDir.resolve("maroctax-core/src/main/java/ma/maroctax/MarocTax.java");
    Files.createDirectories(coreFile.getParent());
    Files.writeString(
        coreFile,
        """
        package ma.maroctax;

        public class MarocTax {}
        """);

    Path apiFile =
        tempDir.resolve("maroctax-api/src/main/java/ma/maroctax/api/App.java");
    Files.createDirectories(apiFile.getParent());
    Files.writeString(
        apiFile,
        """
        package ma.maroctax.api;

        import ma.maroctax.MarocTax;

        public class App {
          private MarocTax tax;
        }
        """);

    List<ScannedFile> files = FileScanner.scan(tempDir);
    DependencyGraph graph = GraphBuilder.build(files);

    assertEquals(2, files.size());
    assertTrue(graph.moduleDependencies().containsKey("maroctax-api"));
    assertEquals(
        List.of("maroctax-core"),
        List.copyOf(graph.moduleDependencies().get("maroctax-api")));
    assertEquals(1, graph.fileDependencies().size());
  }
}
