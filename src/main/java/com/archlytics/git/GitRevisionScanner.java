package com.archlytics.git;

import com.archlytics.config.ArchlyticsConfig;
import com.archlytics.ingest.FileScanner;
import com.archlytics.ingest.ScannedFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class GitRevisionScanner {

  private GitRevisionScanner() {}

  public static List<ScannedFile> scanAtRef(Path repoRoot, String ref, ArchlyticsConfig config) {
    List<ScannedFile> files = new ArrayList<>();

    for (String relativePath : GitRunner.listJavaFilesAtRef(repoRoot, ref)) {
      Path relative = Path.of(relativePath);
      if (FileScanner.matchesAnyPattern(relative, FileScanner.pathMatchers(config.ignore.pathPatterns))) {
        continue;
      }

      String module = FileScanner.inferModule(relative);
      if (config.ignore.modules.contains(module)) {
        continue;
      }

      if (isIgnoredDirectory(relative, config)) {
        continue;
      }

      String source = GitRunner.showFileAtRef(repoRoot, ref, relativePath);
      files.add(
          new ScannedFile(
              repoRoot.resolve(relative), relative, module, source));
    }

    files.sort((a, b) -> a.relativePath().compareTo(b.relativePath()));
    return files;
  }

  private static boolean isIgnoredDirectory(Path relative, ArchlyticsConfig config) {
    for (String part : FileScanner.ignoredDirectories(config)) {
      if (relative.toString().replace('\\', '/').contains("/" + part + "/")) {
        return true;
      }
    }
    return false;
  }
}
