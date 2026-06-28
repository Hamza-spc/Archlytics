package com.archlytics.ingest;

import java.nio.file.Path;

public record ScannedFile(Path absolutePath, Path relativePath, String module) {}
