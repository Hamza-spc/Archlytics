package com.archlytics.pr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.archlytics.config.ArchlyticsConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PullRequestAnalysisTest {

  @TempDir Path tempDir;

  @Test
  void analyze_detectsChangedFilesAndNewModuleEdges() throws Exception {
    initGitRepo();

    PullRequestAnalysis analysis =
        PullRequestAnalysis.analyze(tempDir, "HEAD~1", "HEAD", ArchlyticsConfig.defaults());

    assertEquals("HEAD~1", analysis.baseRef());
    assertEquals("HEAD", analysis.headRef());
    assertTrue(analysis.changedFiles().size() >= 1);
    assertTrue(
        analysis.changedFiles().stream().anyMatch(p -> p.toString().endsWith("App.java")));
    assertTrue(analysis.changedModules().contains("api"));
  }

  private void initGitRepo() throws Exception {
    Path core =
        tempDir.resolve("core/src/main/java/com/example/Core.java");
    Files.createDirectories(core.getParent());
    Files.writeString(core, "package com.example;\npublic class Core {}\n");

    Path app =
        tempDir.resolve("api/src/main/java/com/example/api/App.java");
    Files.createDirectories(app.getParent());
    Files.writeString(
        app,
        """
        package com.example.api;

        import com.example.Core;

        public class App {
          private Core core;
        }
        """);

    runGit("init");
    runGit("config", "user.email", "test@archlytics.dev");
    runGit("config", "user.name", "Archlytics Test");
    runGit("add", ".");
    runGit("commit", "-m", "base");

    Files.writeString(
        app,
        """
        package com.example.api;

        import com.example.Core;
        import com.example.api.dto.One;
        import com.example.api.dto.Two;
        import com.example.api.dto.Three;
        import com.example.api.dto.Four;
        import com.example.api.dto.Five;
        import com.example.api.dto.Six;

        public class App {
          private Core core;
        }
        """);

    Path dto = tempDir.resolve("api/src/main/java/com/example/api/dto/One.java");
    Files.createDirectories(dto.getParent());
    Files.writeString(dto, "package com.example.api.dto;\npublic class One {}\n");
    Files.writeString(
        tempDir.resolve("api/src/main/java/com/example/api/dto/Two.java"),
        "package com.example.api.dto;\npublic class Two {}\n");
    Files.writeString(
        tempDir.resolve("api/src/main/java/com/example/api/dto/Three.java"),
        "package com.example.api.dto;\npublic class Three {}\n");
    Files.writeString(
        tempDir.resolve("api/src/main/java/com/example/api/dto/Four.java"),
        "package com.example.api.dto;\npublic class Four {}\n");
    Files.writeString(
        tempDir.resolve("api/src/main/java/com/example/api/dto/Five.java"),
        "package com.example.api.dto;\npublic class Five {}\n");
    Files.writeString(
        tempDir.resolve("api/src/main/java/com/example/api/dto/Six.java"),
        "package com.example.api.dto;\npublic class Six {}\n");

    runGit("add", ".");
    runGit("commit", "-m", "add dto imports");
  }

  private void runGit(String... args) throws Exception {
    ProcessBuilder builder = new ProcessBuilder();
    java.util.List<String> command = new java.util.ArrayList<>();
    command.add("git");
    command.add("-C");
    command.add(tempDir.toString());
    command.addAll(java.util.List.of(args));
    builder.command(command);
    Process process = builder.start();
    process.waitFor();
    if (process.exitValue() != 0) {
      throw new IllegalStateException("git " + String.join(" ", args) + " failed");
    }
  }
}
